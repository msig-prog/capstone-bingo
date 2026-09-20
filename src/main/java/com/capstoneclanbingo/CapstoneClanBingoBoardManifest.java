package com.capstoneclanbingo;

import java.util.ArrayList;
import java.util.List;

public class CapstoneClanBingoBoardManifest
{
    public String boardId;
    public int version;
    public String imageUrl;
    public int width;
    public int height;
    public List<TileDefinition> tiles = new ArrayList<>();

    public static class TileDefinition
    {
        public int tileNumber;
        public String name;
        public int x;
        public int y;
        public int width;
        public int height;

        public boolean contains(int logicalX, int logicalY)
        {
            return logicalX >= x
                    && logicalX < x + width
                    && logicalY >= y
                    && logicalY < y + height;
        }
    }
}
