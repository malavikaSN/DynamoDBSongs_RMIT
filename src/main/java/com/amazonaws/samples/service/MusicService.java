package com.amazonaws.samples.service;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;

import java.util.ArrayList;
import java.util.List;

public class MusicService {

    private final Table musicTable;

    public MusicService() {
        // Connect to DynamoDB
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(new ProfileCredentialsProvider("default"))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        this.musicTable = dynamoDB.getTable("music");
    }

    // Main query method used by the API
    public List<Item> querySongs(String title, String year, String artist, String album) {

        // artist + year uses LSI
        if (hasValue(artist) && hasValue(year)) {
            return queryByArtistAndYear(artist, year, title, album);
        }

        // title queries use title GSI
        if (hasValue(title)) {
            return queryByTitle(title, artist, year, album);
        }

        // album queries use album GSI
        if (hasValue(album)) {
            return queryByAlbum(album, artist, year);
        }

        // artist only uses main table
        if (hasValue(artist)) {
            return queryByArtist(artist, title, year, album);
        }

        // year only has no direct index, so scan is fallback
        if (hasValue(year)) {
            return scanByYear(year);
        }

        return new ArrayList<>();
    }

    // Query main table by artist
    private List<Item> queryByArtist(String artist, String title, String year, String album) {
        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("artist = :artist")
                .withValueMap(new ValueMap().withString(":artist", artist));

        ItemCollection<QueryOutcome> items = musicTable.query(spec);
        return filterItems(items, title, year, artist, album);
    }

    // Query LSI: artist-year-index
    private List<Item> queryByArtistAndYear(String artist, String year, String title, String album) {
        Index index = musicTable.getIndex("artist-year-index");

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("artist = :artist and #yr = :year")
                .withNameMap(new NameMap().with("#yr", "year"))
                .withValueMap(new ValueMap()
                        .withString(":artist", artist)
                        .withString(":year", year));

        ItemCollection<QueryOutcome> items = index.query(spec);
        return filterItems(items, title, year, artist, album);
    }

    // Query GSI: title-artist-index
    private List<Item> queryByTitle(String title, String artist, String year, String album) {
        Index index = musicTable.getIndex("title-artist-index");

        QuerySpec spec;

        if (hasValue(artist)) {
            spec = new QuerySpec()
                    .withKeyConditionExpression("title = :title and artist = :artist")
                    .withValueMap(new ValueMap()
                            .withString(":title", title)
                            .withString(":artist", artist));
        } else {
            spec = new QuerySpec()
                    .withKeyConditionExpression("title = :title")
                    .withValueMap(new ValueMap().withString(":title", title));
        }

        ItemCollection<QueryOutcome> items = index.query(spec);
        return filterItems(items, title, year, artist, album);
    }

    // Query GSI: album-artist-index
    private List<Item> queryByAlbum(String album, String artist, String year) {
        Index index = musicTable.getIndex("album-artist-index");

        QuerySpec spec;

        if (hasValue(artist)) {
            spec = new QuerySpec()
                    .withKeyConditionExpression("album = :album and artist = :artist")
                    .withValueMap(new ValueMap()
                            .withString(":album", album)
                            .withString(":artist", artist));
        } else {
            spec = new QuerySpec()
                    .withKeyConditionExpression("album = :album")
                    .withValueMap(new ValueMap().withString(":album", album));
        }

        ItemCollection<QueryOutcome> items = index.query(spec);
        return filterItems(items, null, year, artist, album);
    }

    // Scan fallback for year only
    private List<Item> scanByYear(String year) {
        ScanSpec spec = new ScanSpec()
                .withFilterExpression("#yr = :year")
                .withNameMap(new NameMap().with("#yr", "year"))
                .withValueMap(new ValueMap().withString(":year", year));

        ItemCollection<ScanOutcome> items = musicTable.scan(spec);

        List<Item> results = new ArrayList<>();
        for (Item item : items) {
            results.add(item);
        }

        return results;
    }

    // Apply AND filtering for extra fields
    private List<Item> filterItems(Iterable<Item> items, String title, String year, String artist, String album) {
        List<Item> results = new ArrayList<>();

        for (Item item : items) {
            if (hasValue(title) && !title.equals(item.getString("title"))) {
                continue;
            }

            if (hasValue(year) && !year.equals(item.getString("year"))) {
                continue;
            }

            if (hasValue(artist) && !artist.equals(item.getString("artist"))) {
                continue;
            }

            if (hasValue(album) && !album.equals(item.getString("album"))) {
                continue;
            }

            results.add(item);
        }

        return results;
    }

    // Check if input is not empty
    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }
}