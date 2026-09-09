package com.capstoneclanbingo;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CapstoneClanBingoApiClient
{
    /*
     * Live Cloudflare Worker.
     *
     * Do not include a trailing slash here because
     * each endpoint below begins with "/api/...".
     */
    private static final String API_BASE_URL =
            "https://capstone-bingo-api.levinsteel.workers.dev";

    private static final MediaType JSON =
            MediaType.parse(
                    "application/json; charset=utf-8"
            );

    private final OkHttpClient httpClient;
    private final Gson gson;

    public CapstoneClanBingoApiClient(
            OkHttpClient httpClient,
            Gson gson
    )
    {
        this.httpClient =
                httpClient;

        this.gson =
                gson;
    }

    public void validateTeamCode(
            String teamCode,
            TeamValidationCallback callback
    )
    {
        Request request =
                new Request.Builder()
                        .url(
                                API_BASE_URL
                                        + "/api/team/"
                                        + teamCode
                        )
                        .get()
                        .build();

        httpClient
                .newCall(request)
                .enqueue(
                        new Callback()
                        {
                            @Override
                            public void onFailure(
                                    Call call,
                                    IOException exception
                            )
                            {
                                runOnSwingThread(() ->
                                        callback.onResult(
                                                false,
                                                "Could not connect to bingo server."
                                        )
                                );
                            }

                            @Override
                            public void onResponse(
                                    Call call,
                                    Response response
                            )
                            {
                                try (Response ignored = response)
                                {
                                    if (response.isSuccessful())
                                    {
                                        runOnSwingThread(() ->
                                                callback.onResult(
                                                        true,
                                                        "Team found."
                                                )
                                        );

                                        return;
                                    }

                                    if (response.code() == 404)
                                    {
                                        runOnSwingThread(() ->
                                                callback.onResult(
                                                        false,
                                                        "Team code not found."
                                                )
                                        );

                                        return;
                                    }

                                    runOnSwingThread(() ->
                                            callback.onResult(
                                                    false,
                                                    "Server returned error "
                                                            + response.code()
                                                            + "."
                                            )
                                    );
                                }
                            }
                        }
                );
    }

    public void loadBoard(
            String teamCode,
            BoardCallback callback
    )
    {
        Request request =
                new Request.Builder()
                        .url(
                                API_BASE_URL
                                        + "/api/team/"
                                        + teamCode
                                        + "/board"
                        )
                        .get()
                        .build();

        httpClient
                .newCall(request)
                .enqueue(
                        new Callback()
                        {
                            @Override
                            public void onFailure(
                                    Call call,
                                    IOException exception
                            )
                            {
                                runOnSwingThread(() ->
                                        callback.onResult(
                                                false,
                                                null,
                                                "Could not connect to bingo server."
                                        )
                                );
                            }

                            @Override
                            public void onResponse(
                                    Call call,
                                    Response response
                            )
                            {
                                try (Response ignored = response)
                                {
                                    ResponseBody responseBody =
                                            response.body();

                                    String json =
                                            responseBody == null
                                                    ? ""
                                                    : responseBody.string();

                                    if (!response.isSuccessful())
                                    {
                                        runOnSwingThread(() ->
                                                callback.onResult(
                                                        false,
                                                        null,
                                                        response.code() == 404
                                                                ? "Team code not found."
                                                                : "Server returned error "
                                                                + response.code()
                                                                + "."
                                                )
                                        );

                                        return;
                                    }

                                    try
                                    {
                                        BoardResponse boardResponse =
                                                gson.fromJson(
                                                        json,
                                                        BoardResponse.class
                                                );

                                        if (
                                                boardResponse == null
                                                        || !boardResponse.success
                                                        || boardResponse.tiles == null
                                        )
                                        {
                                            runOnSwingThread(() ->
                                                    callback.onResult(
                                                            false,
                                                            null,
                                                            "Server returned an invalid board."
                                                    )
                                            );

                                            return;
                                        }

                                        normalizeBoard(
                                                boardResponse
                                        );

                                        runOnSwingThread(() ->
                                                callback.onResult(
                                                        true,
                                                        boardResponse,
                                                        "Board loaded."
                                                )
                                        );
                                    }
                                    catch (Exception exception)
                                    {
                                        runOnSwingThread(() ->
                                                callback.onResult(
                                                        false,
                                                        null,
                                                        "Could not read board data."
                                                )
                                        );
                                    }
                                }
                                catch (IOException exception)
                                {
                                    runOnSwingThread(() ->
                                            callback.onResult(
                                                    false,
                                                    null,
                                                    "Could not read server response."
                                            )
                                    );
                                }
                            }
                        }
                );
    }

    public void saveTile(
            String teamCode,
            TileData tile,
            TileSaveCallback callback
    )
    {
        TileUpdatePayload payload =
                new TileUpdatePayload();

        payload.status =
                tile.status;

        payload.owner =
                tile.owner;

        payload.progress =
                tile.progress;

        payload.aidRequested =
                tile.aidRequested;

        payload.helpers =
                tile.helpers == null
                        ? new ArrayList<>()
                        : new ArrayList<>(
                        tile.helpers
                );

        String json =
                gson.toJson(
                        payload
                );

        RequestBody requestBody =
                RequestBody.create(
                        JSON,
                        json
                );

        Request request =
                new Request.Builder()
                        .url(
                                API_BASE_URL
                                        + "/api/team/"
                                        + teamCode
                                        + "/tile/"
                                        + tile.tileNumber
                        )
                        .put(
                                requestBody
                        )
                        .build();

        httpClient
                .newCall(request)
                .enqueue(
                        new Callback()
                        {
                            @Override
                            public void onFailure(
                                    Call call,
                                    IOException exception
                            )
                            {
                                runOnSwingThread(() ->
                                        callback.onResult(
                                                false,
                                                null,
                                                "Could not connect to bingo server."
                                        )
                                );
                            }

                            @Override
                            public void onResponse(
                                    Call call,
                                    Response response
                            )
                            {
                                try (Response ignored = response)
                                {
                                    ResponseBody responseBody =
                                            response.body();

                                    String responseJson =
                                            responseBody == null
                                                    ? ""
                                                    : responseBody.string();

                                    if (!response.isSuccessful())
                                    {
                                        runOnSwingThread(() ->
                                                callback.onResult(
                                                        false,
                                                        null,
                                                        "Server returned error "
                                                                + response.code()
                                                                + "."
                                                )
                                        );

                                        return;
                                    }

                                    try
                                    {
                                        TileSaveResponse saveResponse =
                                                gson.fromJson(
                                                        responseJson,
                                                        TileSaveResponse.class
                                                );

                                        if (
                                                saveResponse == null
                                                        || !saveResponse.success
                                                        || saveResponse.tile == null
                                        )
                                        {
                                            runOnSwingThread(() ->
                                                    callback.onResult(
                                                            false,
                                                            null,
                                                            "Server returned an invalid tile."
                                                    )
                                            );

                                            return;
                                        }

                                        normalizeTile(
                                                saveResponse.tile
                                        );

                                        runOnSwingThread(() ->
                                                callback.onResult(
                                                        true,
                                                        saveResponse.tile,
                                                        "Tile saved."
                                                )
                                        );
                                    }
                                    catch (Exception exception)
                                    {
                                        runOnSwingThread(() ->
                                                callback.onResult(
                                                        false,
                                                        null,
                                                        "Could not read saved tile."
                                                )
                                        );
                                    }
                                }
                                catch (IOException exception)
                                {
                                    runOnSwingThread(() ->
                                            callback.onResult(
                                                    false,
                                                    null,
                                                    "Could not read server response."
                                            )
                                    );
                                }
                            }
                        }
                );
    }

    private void normalizeBoard(
            BoardResponse board
    )
    {
        for (
                TileData tile
                : board.tiles
        )
        {
            normalizeTile(
                    tile
            );
        }
    }

    private void normalizeTile(
            TileData tile
    )
    {
        if (tile.status == null)
        {
            tile.status =
                    "OPEN";
        }

        if (tile.helpers == null)
        {
            tile.helpers =
                    new ArrayList<>();
        }
    }

    private void runOnSwingThread(
            Runnable runnable
    )
    {
        SwingUtilities.invokeLater(
                runnable
        );
    }

    public interface TeamValidationCallback
    {
        void onResult(
                boolean success,
                String message
        );
    }

    public interface BoardCallback
    {
        void onResult(
                boolean success,
                BoardResponse board,
                String message
        );
    }

    public interface TileSaveCallback
    {
        void onResult(
                boolean success,
                TileData tile,
                String message
        );
    }

    public static class BoardResponse
    {
        public boolean success;
        public TeamData team;
        public List<TileData> tiles;
    }

    public static class TeamData
    {
        public int id;
        public String code;
        public String name;
    }

    public static class TileData
    {
        public int tileNumber;

        public String status;

        public String owner;

        public String progress;

        public boolean aidRequested;

        public List<String> helpers;

        public String updatedAt;

        public TileData copy()
        {
            TileData copy =
                    new TileData();

            copy.tileNumber =
                    tileNumber;

            copy.status =
                    status;

            copy.owner =
                    owner;

            copy.progress =
                    progress;

            copy.aidRequested =
                    aidRequested;

            copy.helpers =
                    helpers == null
                            ? new ArrayList<>()
                            : new ArrayList<>(
                            helpers
                    );

            copy.updatedAt =
                    updatedAt;

            return copy;
        }
    }

    private static class TileUpdatePayload
    {
        String status;

        String owner;

        String progress;

        boolean aidRequested;

        List<String> helpers;
    }

    private static class TileSaveResponse
    {
        boolean success;

        TileData tile;
    }
}