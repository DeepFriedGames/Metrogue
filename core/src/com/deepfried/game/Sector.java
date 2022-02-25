package com.deepfried.game;

import com.badlogic.gdx.utils.Array;

public class Sector {
    public int order;
	public int index;
    public int mapX, mapY;
    public Array<Feature> features;

    public enum Feature {
	//TODO move this up a level
        ENTRANCE {
            @Override
            void generate(Sector sector) {

            }
        },
        OPEN_L {
            @Override
            void generate(Sector sector) {

            }
        },
        OPEN_U {
            @Override
            void generate(Sector sector) {

            }
        },
        OPEN_R {
            @Override
            void generate(Sector sector) {

            }
        },
        OPEN_D {
            @Override
            void generate(Sector sector) {

            }
        },
        EXIT {
            @Override
            void generate(Sector sector) {

            }
        },
        MORPH_ITEM {
            @Override
            void generate(Sector sector) {

            }
        },
        MORPH_OBSTACLE {
            @Override
            void generate(Sector sector) {

            }
        },
        DEFAULT_ITEM {
            @Override
            void generate(Sector sector) {

            }
        },
        DEFAULT_OBSTACLE {
            @Override
            void generate(Sector sector) {

            }
        },
        START {
            @Override
            void generate(Sector sector) {

            }
        };

        abstract void generate(Sector sector);
		
		static Feature getItemFeature(KeyItem item) {
			switch(item) {
				case(MORPH_BALL):
					return MORPH_ITEM;
				default:
					return DEFAULT_ITEM;
			}
		}
		
		static Feature getItemObstacle(KeyItem item) {
			switch(item) {
				case(MORPH_BALL):
					return MORPH_OBSTACLE;
				default:
					return DEFAULT_OBSTACLE;
			}
		}

        public class START {
        }
    }
}
