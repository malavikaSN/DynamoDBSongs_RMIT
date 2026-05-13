import json
import base64
import time
import boto3
from boto3.dynamodb.conditions import Key, Attr
from decimal import Decimal

dynamodb = boto3.resource("dynamodb", region_name="us-east-1")

login_table = dynamodb.Table("login")
music_table = dynamodb.Table("music")
subscriptions_table = dynamodb.Table("subscriptions")


def json_default(obj):
    # Convert DynamoDB Decimal values to normal numbers
    if isinstance(obj, Decimal):
        if obj % 1 == 0:
            return int(obj)
        return float(obj)
    raise TypeError


def response(status_code, body):
    # Return API Gateway response with CORS headers
    return {
        "statusCode": status_code,
        "headers": {
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Headers": "Content-Type,Authorization",
            "Access-Control-Allow-Methods": "GET,POST,PUT,DELETE,OPTIONS",
            "Content-Type": "application/json"
        },
        "body": json.dumps(body, default=json_default)
    }


def parse_body(event):
    # Parse JSON request body safely
    raw_body = event.get("body")

    if not raw_body:
        return {}

    if event.get("isBase64Encoded"):
        raw_body = base64.b64decode(raw_body).decode("utf-8")

    try:
        return json.loads(raw_body)
    except Exception:
        return {}


def get_query_params(event):
    # Get query string parameters
    return event.get("queryStringParameters") or {}


def get_path(event):
    # Get request path from API Gateway
    return event.get("path") or event.get("rawPath") or "/"


def get_method(event):
    # Get HTTP method from API Gateway
    return (event.get("httpMethod") or event.get("requestContext", {}).get("http", {}).get("method") or "GET").upper()


def build_songkey(title, album, year):
    # Build a stable song key if frontend does not send one
    safe_title = title or "UnknownTitle"
    safe_album = album or "UnknownAlbum"
    safe_year = year or "UnknownYear"
    return f"{safe_title}#{safe_album}#{safe_year}"


def clean_song(item):
    # Return song fields in frontend-friendly format
    return {
        "artist": item.get("artist", ""),
        "songkey": item.get("songkey", ""),
        "title": item.get("title", ""),
        "album": item.get("album", ""),
        "year": item.get("year", ""),
        "image_url": item.get("image_url", "")
    }


def handle_health():
    return response(200, {
        "status": "ok",
        "backend": "lambda"
    })


# -------------------------
# Login table CRUD
# -------------------------

def handle_register(body):
    email = body.get("email")
    user_name = body.get("user_name") or body.get("username")
    password = body.get("password")

    if not email or not user_name or not password:
        return response(400, {
            "success": False,
            "message": "email, user_name and password are required"
        })

    existing = login_table.get_item(Key={"email": email}).get("Item")

    if existing:
        return response(409, {
            "success": False,
            "message": "The email already exists"
        })

    login_table.put_item(Item={
        "email": email,
        "user_name": user_name,
        "password": password
    })

    return response(200, {
        "success": True,
        "message": "registered"
    })


def handle_login(body):
    email = body.get("email")
    password = body.get("password")

    if not email or not password:
        return response(400, {
            "success": False,
            "message": "email and password are required"
        })

    item = login_table.get_item(Key={"email": email}).get("Item")

    if not item or item.get("password") != password:
        return response(401, {
            "success": False,
            "message": "email or password is invalid"
        })

    token = f"lambda-demo-token-{int(time.time())}"

    return response(200, {
        "success": True,
        "message": "login successful",
        "token": token,
        "email": item.get("email"),
        "user_name": item.get("user_name", "")
    })


def handle_get_user(params):
    email = params.get("email")

    if not email:
        return response(400, {
            "success": False,
            "message": "email is required"
        })

    item = login_table.get_item(Key={"email": email}).get("Item")

    if not item:
        return response(404, {
            "success": False,
            "message": "user not found"
        })

    item.pop("password", None)

    return response(200, {
        "success": True,
        "user": item
    })


def handle_update_user(body):
    email = body.get("email")

    if not email:
        return response(400, {
            "success": False,
            "message": "email is required"
        })

    update_parts = []
    values = {}
    names = {}

    if body.get("user_name") is not None:
        update_parts.append("#user_name = :user_name")
        names["#user_name"] = "user_name"
        values[":user_name"] = body.get("user_name")

    if body.get("password") is not None:
        update_parts.append("#password = :password")
        names["#password"] = "password"
        values[":password"] = body.get("password")

    if not update_parts:
        return response(400, {
            "success": False,
            "message": "no fields to update"
        })

    login_table.update_item(
        Key={"email": email},
        UpdateExpression="SET " + ", ".join(update_parts),
        ExpressionAttributeNames=names,
        ExpressionAttributeValues=values
    )

    return response(200, {
        "success": True,
        "message": "user updated"
    })


def handle_delete_user(body, params):
    email = body.get("email") or params.get("email")

    if not email:
        return response(400, {
            "success": False,
            "message": "email is required"
        })

    login_table.delete_item(Key={"email": email})

    return response(200, {
        "success": True,
        "message": "user deleted"
    })


# -------------------------
# Music table CRUD
# -------------------------

def handle_get_songs(params):
    title = params.get("title")
    year = params.get("year")
    artist = params.get("artist")
    album = params.get("album")

    if not any([title, year, artist, album]):
        return response(400, {
            "success": False,
            "message": "At least one query field is required"
        })

    if artist:
        result = music_table.query(
            KeyConditionExpression=Key("artist").eq(artist)
        )
        items = result.get("Items", [])
    else:
        result = music_table.scan()
        items = result.get("Items", [])

    filtered = []

    for item in items:
        if title and item.get("title") != title:
            continue
        if year and item.get("year") != year:
            continue
        if album and item.get("album") != album:
            continue
        filtered.append(clean_song(item))

    return response(200, {
        "success": True,
        "songs": filtered
    })


def handle_create_song(body):
    artist = body.get("artist")
    title = body.get("title")
    album = body.get("album")
    year = body.get("year")
    image_url = body.get("image_url", "")
    songkey = body.get("songkey") or build_songkey(title, album, year)

    if not artist or not songkey:
        return response(400, {
            "success": False,
            "message": "artist and songkey are required"
        })

    item = {
        "artist": artist,
        "songkey": songkey,
        "title": title or "",
        "album": album or "",
        "year": year or "",
        "image_url": image_url
    }

    music_table.put_item(Item=item)

    return response(200, {
        "success": True,
        "message": "song created",
        "song": item
    })


def handle_update_song(body):
    artist = body.get("artist")
    songkey = body.get("songkey")

    if not artist or not songkey:
        return response(400, {
            "success": False,
            "message": "artist and songkey are required"
        })

    allowed_fields = ["title", "album", "year", "image_url"]
    update_parts = []
    values = {}
    names = {}

    for field in allowed_fields:
        if body.get(field) is not None:
            name_key = f"#{field}"
            value_key = f":{field}"
            update_parts.append(f"{name_key} = {value_key}")
            names[name_key] = field
            values[value_key] = body.get(field)

    if not update_parts:
        return response(400, {
            "success": False,
            "message": "no fields to update"
        })

    music_table.update_item(
        Key={
            "artist": artist,
            "songkey": songkey
        },
        UpdateExpression="SET " + ", ".join(update_parts),
        ExpressionAttributeNames=names,
        ExpressionAttributeValues=values
    )

    return response(200, {
        "success": True,
        "message": "song updated"
    })


def handle_delete_song(body, params):
    artist = body.get("artist") or params.get("artist")
    songkey = body.get("songkey") or params.get("songkey")

    if not artist or not songkey:
        return response(400, {
            "success": False,
            "message": "artist and songkey are required"
        })

    music_table.delete_item(Key={
        "artist": artist,
        "songkey": songkey
    })

    return response(200, {
        "success": True,
        "message": "song deleted"
    })


# -------------------------
# Subscription functions
# -------------------------

def handle_get_subscriptions(params):
    email = params.get("email")

    if not email:
        return response(400, {
            "success": False,
            "message": "email is required"
        })

    result = subscriptions_table.query(
        KeyConditionExpression=Key("email").eq(email)
    )

    subscriptions = [clean_song(item) for item in result.get("Items", [])]

    return response(200, {
        "success": True,
        "subscriptions": subscriptions
    })


def handle_create_subscription(body):
    email = body.get("email")
    songkey = body.get("songkey")
    artist = body.get("artist")

    if not email or not songkey:
        return response(400, {
            "success": False,
            "message": "email and songkey are required"
        })

    item = {
        "email": email,
        "songkey": songkey,
        "title": body.get("title", ""),
        "artist": artist or "",
        "album": body.get("album", ""),
        "year": body.get("year", ""),
        "image_url": body.get("image_url", "")
    }

    subscriptions_table.put_item(Item=item)

    return response(200, {
        "success": True,
        "message": "subscription added"
    })


def handle_delete_subscription(body, params):
    email = body.get("email") or params.get("email")
    songkey = body.get("songkey") or params.get("songkey")

    if not email or not songkey:
        return response(400, {
            "success": False,
            "message": "email and songkey are required"
        })

    subscriptions_table.delete_item(Key={
        "email": email,
        "songkey": songkey
    })

    return response(200, {
        "success": True,
        "message": "subscription removed"
    })


def lambda_handler(event, context):
    method = get_method(event)
    path = get_path(event)
    params = get_query_params(event)
    body = parse_body(event)

    if method == "OPTIONS":
        return response(200, {
            "success": True,
            "message": "CORS preflight ok"
        })

    if path == "/health" and method == "GET":
        return handle_health()

    if path == "/api/register" and method == "POST":
        return handle_register(body)

    if path == "/api/login" and method == "POST":
        return handle_login(body)

    if path == "/api/users" and method == "GET":
        return handle_get_user(params)

    if path == "/api/users" and method == "PUT":
        return handle_update_user(body)

    if path == "/api/users" and method == "DELETE":
        return handle_delete_user(body, params)

    if path == "/api/songs" and method == "GET":
        return handle_get_songs(params)

    if path == "/api/songs/query" and method == "POST":
        return handle_get_songs(body)

    if path == "/api/songs" and method == "POST":
        return handle_create_song(body)

    if path == "/api/songs" and method == "PUT":
        return handle_update_song(body)

    if path == "/api/songs" and method == "DELETE":
        return handle_delete_song(body, params)

    if path == "/api/subscriptions" and method == "GET":
        return handle_get_subscriptions(params)

    if path == "/api/subscriptions" and method == "POST":
        return handle_create_subscription(body)

    if path == "/api/subscriptions" and method == "DELETE":
        return handle_delete_subscription(body, params)

    return response(404, {
        "success": False,
        "message": f"No route for {method} {path}"
    })
