package com.capstoneclanbingo;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
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
    public static final String API_BASE_URL =
            "https://capstone-bingo-api.levinsteel.workers.dev";

    private static final String BOARD_MANIFEST_URL =
            API_BASE_URL + "/api/board/current";

    private static final String ALLOWED_BOARD_HOST =
            "capstone-bingo-api.levinsteel.workers.dev";

    private static final int MAX_BOARD_DIMENSION = 4096;
    private static final int MAX_TILE_COUNT = 100;

    private static final MediaType JSON =
            MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;

    private String cachedImageKey;
    private BufferedImage cachedImage;

    public CapstoneClanBingoApiClient(
            OkHttpClient httpClient,
            Gson gson
    )
    {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public void validateTeamCode(
            String teamCode,
            TeamValidationCallback callback
    )
    {
        Request request = new Request.Builder()
                .url(API_BASE_URL + "/api/team/" + teamCode)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                swing(() ->
                        callback.onResult(
                                false,
                                "Could not connect to bingo server."
                        )
                );
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response ignored = response)
                {
                    if (response.isSuccessful())
                    {
                        swing(() ->
                                callback.onResult(true, "Team found.")
                        );
                        return;
                    }

                    swing(() ->
                            callback.onResult(
                                    false,
                                    response.code() == 404
                                            ? "Team code not found."
                                            : "Server returned error "
                                            + response.code()
                                            + "."
                            )
                    );
                }
            }
        });
    }

    public void loadBoardManifest(
            ManifestCallback callback
    )
    {
        Request request = new Request.Builder()
                .url(BOARD_MANIFEST_URL)
                .header("Cache-Control", "no-cache")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                swing(() ->
                        callback.onResult(
                                false,
                                null,
                                "Could not load bingo board definition."
                        )
                );
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response ignored = response)
                {
                    if (!response.isSuccessful())
                    {
                        swing(() ->
                                callback.onResult(
                                        false,
                                        null,
                                        "Board definition returned error "
                                                + response.code()
                                                + "."
                                )
                        );
                        return;
                    }

                    ResponseBody body = response.body();

                    if (body == null)
                    {
                        swing(() ->
                                callback.onResult(
                                        false,
                                        null,
                                        "Board definition was empty."
                                )
                        );
                        return;
                    }

                    try
                    {
                        CapstoneClanBingoBoardManifest manifest =
                                gson.fromJson(
                                        body.string(),
                                        CapstoneClanBingoBoardManifest.class
                                );

                        String validationError =
                                validateManifest(manifest);

                        if (validationError != null)
                        {
                            swing(() ->
                                    callback.onResult(
                                            false,
                                            null,
                                            validationError
                                    )
                            );
                            return;
                        }

                        swing(() ->
                                callback.onResult(
                                        true,
                                        manifest,
                                        "Board definition loaded."
                                )
                        );
                    }
                    catch (Exception exception)
                    {
                        swing(() ->
                                callback.onResult(
                                        false,
                                        null,
                                        "Could not read bingo board definition."
                                )
                        );
                    }
                }
            }
        });
    }

    public void loadBoardImage(
            CapstoneClanBingoBoardManifest manifest,
            BoardImageCallback callback
    )
    {
        String cacheKey =
                manifest.boardId
                        + ":"
                        + manifest.version
                        + ":"
                        + manifest.imageUrl;

        synchronized (this)
        {
            if (
                    cachedImage != null
                            && cacheKey.equals(cachedImageKey)
            )
            {
                BufferedImage image = cachedImage;

                swing(() ->
                        callback.onResult(
                                true,
                                image,
                                "Board image loaded from cache."
                        )
                );
                return;
            }
        }

        Request request = new Request.Builder()
                .url(manifest.imageUrl)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                swing(() ->
                        callback.onResult(
                                false,
                                null,
                                "Could not download bingo board image."
                        )
                );
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response ignored = response)
                {
                    if (!response.isSuccessful())
                    {
                        swing(() ->
                                callback.onResult(
                                        false,
                                        null,
                                        "Board image returned error "
                                                + response.code()
                                                + "."
                                )
                        );
                        return;
                    }

                    ResponseBody body = response.body();

                    if (body == null)
                    {
                        swing(() ->
                                callback.onResult(
                                        false,
                                        null,
                                        "Board image response was empty."
                                )
                        );
                        return;
                    }

                    BufferedImage image =
                            ImageIO.read(body.byteStream());

                    if (image == null)
                    {
                        swing(() ->
                                callback.onResult(
                                        false,
                                        null,
                                        "Downloaded board image was invalid."
                                )
                        );
                        return;
                    }

                    if (
                            image.getWidth() != manifest.width
                                    || image.getHeight() != manifest.height
                    )
                    {
                        swing(() ->
                                callback.onResult(
                                        false,
                                        null,
                                        "Board image dimensions do not match its definition."
                                )
                        );
                        return;
                    }

                    synchronized (CapstoneClanBingoApiClient.this)
                    {
                        cachedImageKey = cacheKey;
                        cachedImage = image;
                    }

                    swing(() ->
                            callback.onResult(
                                    true,
                                    image,
                                    "Board image loaded."
                            )
                    );
                }
                catch (IOException exception)
                {
                    swing(() ->
                            callback.onResult(
                                    false,
                                    null,
                                    "Could not decode bingo board image."
                            )
                    );
                }
            }
        });
    }

    public void loadBoard(
            String teamCode,
            BoardCallback callback
    )
    {
        Request request = new Request.Builder()
                .url(API_BASE_URL + "/api/team/" + teamCode + "/board")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                swing(() ->
                        callback.onResult(
                                false,
                                null,
                                "Could not connect to bingo server."
                        )
                );
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response ignored = response)
                {
                    ResponseBody body = response.body();
                    String json =
                            body == null ? "" : body.string();

                    if (!response.isSuccessful())
                    {
                        swing(() ->
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
                        BoardResponse board =
                                gson.fromJson(
                                        json,
                                        BoardResponse.class
                                );

                        if (
                                board == null
                                        || !board.success
                                        || board.tiles == null
                        )
                        {
                            swing(() ->
                                    callback.onResult(
                                            false,
                                            null,
                                            "Server returned an invalid board."
                                    )
                            );
                            return;
                        }

                        normalizeBoard(board);

                        swing(() ->
                                callback.onResult(
                                        true,
                                        board,
                                        "Board loaded."
                                )
                        );
                    }
                    catch (Exception exception)
                    {
                        swing(() ->
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
                    swing(() ->
                            callback.onResult(
                                    false,
                                    null,
                                    "Could not read server response."
                            )
                    );
                }
            }
        });
    }

    public void saveTile(
            String teamCode,
            String actor,
            TileData tile,
            boolean overwriteOwner,
            TileSaveCallback callback
    )
    {
        TileUpdatePayload payload =
                new TileUpdatePayload();

        payload.actor = actor;
        payload.overwriteOwner = overwriteOwner;
        payload.status = tile.status;
        payload.owner = tile.owner;
        payload.progress = tile.progress;
        payload.aidRequested = tile.aidRequested;
        payload.aidNeeded = tile.aidNeeded;
        payload.helpers =
                tile.helpers == null
                        ? new ArrayList<>()
                        : new ArrayList<>(tile.helpers);

        RequestBody body =
                RequestBody.create(
                        JSON,
                        gson.toJson(payload)
                );

        Request request = new Request.Builder()
                .url(
                        API_BASE_URL
                                + "/api/team/"
                                + teamCode
                                + "/tile/"
                                + tile.tileNumber
                )
                .put(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                swing(() ->
                        callback.onResult(
                                false,
                                null,
                                "Could not connect to bingo server."
                        )
                );
            }

            @Override
            public void onResponse(Call call, Response response)
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
                        String message =
                                "Server returned error "
                                        + response.code()
                                        + ".";

                        try
                        {
                            ErrorResponse error =
                                    gson.fromJson(
                                            json,
                                            ErrorResponse.class
                                    );

                            if (
                                    error != null
                                            && error.error != null
                                            && !error.error.trim().isEmpty()
                            )
                            {
                                message = error.error;
                            }
                        }
                        catch (Exception ignoredError)
                        {
                            // Keep generic message.
                        }

                        String finalMessage = message;

                        swing(() ->
                                callback.onResult(
                                        false,
                                        null,
                                        finalMessage
                                )
                        );
                        return;
                    }

                    try
                    {
                        TileSaveResponse saved =
                                gson.fromJson(
                                        json,
                                        TileSaveResponse.class
                                );

                        if (
                                saved == null
                                        || !saved.success
                                        || saved.tile == null
                        )
                        {
                            swing(() ->
                                    callback.onResult(
                                            false,
                                            null,
                                            "Server returned an invalid tile."
                                    )
                            );
                            return;
                        }

                        normalizeTile(saved.tile);

                        swing(() ->
                                callback.onResult(
                                        true,
                                        saved.tile,
                                        "Tile saved."
                                )
                        );
                    }
                    catch (Exception exception)
                    {
                        swing(() ->
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
                    swing(() ->
                            callback.onResult(
                                    false,
                                    null,
                                    "Could not read server response."
                            )
                    );
                }
            }
        });
    }

    private String validateManifest(
            CapstoneClanBingoBoardManifest manifest
    )
    {
        if (manifest == null)
        {
            return "Board definition was missing.";
        }

        if (
                manifest.boardId == null
                        || manifest.boardId.trim().isEmpty()
        )
        {
            return "Board definition has no board ID.";
        }

        if (
                manifest.version < 1
                        || manifest.width < 1
                        || manifest.height < 1
                        || manifest.width > MAX_BOARD_DIMENSION
                        || manifest.height > MAX_BOARD_DIMENSION
        )
        {
            return "Board definition has invalid dimensions or version.";
        }

        if (
                manifest.tiles == null
                        || manifest.tiles.isEmpty()
                        || manifest.tiles.size() > MAX_TILE_COUNT
        )
        {
            return "Board definition has an invalid tile count.";
        }

        try
        {
            URI imageUri =
                    URI.create(manifest.imageUrl);

            if (
                    !"https".equalsIgnoreCase(imageUri.getScheme())
                            || !ALLOWED_BOARD_HOST.equalsIgnoreCase(
                            imageUri.getHost()
                    )
            )
            {
                return "Board image URL is not an approved Capstone host.";
            }
        }
        catch (Exception exception)
        {
            return "Board definition has an invalid image URL.";
        }

        Set<Integer> seenTileNumbers =
                new HashSet<>();

        for (
                CapstoneClanBingoBoardManifest.TileDefinition tile
                : manifest.tiles
        )
        {
            if (
                    tile == null
                            || tile.tileNumber < 1
                            || !seenTileNumbers.add(tile.tileNumber)
            )
            {
                return "Board definition has duplicate or invalid tile numbers.";
            }

            if (
                    tile.name == null
                            || tile.name.trim().isEmpty()
                            || tile.x < 0
                            || tile.y < 0
                            || tile.width < 1
                            || tile.height < 1
                            || tile.x + tile.width > manifest.width
                            || tile.y + tile.height > manifest.height
            )
            {
                return "Board definition contains an invalid tile rectangle.";
            }
        }

        return null;
    }

    private void normalizeBoard(BoardResponse board)
    {
        for (TileData tile : board.tiles)
        {
            normalizeTile(tile);
        }
    }

    private void normalizeTile(TileData tile)
    {
        if (tile.status == null)
        {
            tile.status = "OPEN";
        }

        if (tile.helpers == null)
        {
            tile.helpers = new ArrayList<>();
        }
    }

    private void swing(Runnable runnable)
    {
        SwingUtilities.invokeLater(runnable);
    }

    public interface TeamValidationCallback
    {
        void onResult(
                boolean success,
                String message
        );
    }

    public interface ManifestCallback
    {
        void onResult(
                boolean success,
                CapstoneClanBingoBoardManifest manifest,
                String message
        );
    }

    public interface BoardImageCallback
    {
        void onResult(
                boolean success,
                BufferedImage image,
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
        public Integer aidNeeded;
        public List<String> helpers;
        public String updatedAt;

        public TileData copy()
        {
            TileData copy = new TileData();

            copy.tileNumber = tileNumber;
            copy.status = status;
            copy.owner = owner;
            copy.progress = progress;
            copy.aidRequested = aidRequested;
            copy.aidNeeded = aidNeeded;
            copy.helpers =
                    helpers == null
                            ? new ArrayList<>()
                            : new ArrayList<>(helpers);
            copy.updatedAt = updatedAt;

            return copy;
        }
    }

    private static class TileUpdatePayload
    {
        String actor;
        boolean overwriteOwner;
        String status;
        String owner;
        String progress;
        boolean aidRequested;
        Integer aidNeeded;
        List<String> helpers;
    }

    private static class TileSaveResponse
    {
        boolean success;
        TileData tile;
    }

    private static class ErrorResponse
    {
        String error;
    }
}
