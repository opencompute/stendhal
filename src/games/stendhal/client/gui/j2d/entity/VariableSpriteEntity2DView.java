package games.stendhal.client.gui.j2d.entity;

import games.stendhal.client.IGameScreen;
import games.stendhal.client.ZoneInfo;
import games.stendhal.client.entity.StatefulEntity;
import games.stendhal.client.sprite.Sprite;
import games.stendhal.client.sprite.SpriteStore;

public class VariableSpriteEntity2DView<T extends StatefulEntity> extends Entity2DView<T> {
	
	@Override
	protected void buildRepresentation(T entity) {
		final SpriteStore store = SpriteStore.get();
		Sprite sprite;
		ZoneInfo info = ZoneInfo.get();
		
		String entityType = entity.getType();
		if (entityType != null) {
			if (entity.getName() == null) {
				sprite = store.getSprite(translate("signs/transparent"));
			} else {
				sprite = store.getModifiedSprite(translate(getClassResourcePath() + "/" + entity.getName()),
						info.getZoneColor(), info.getColorMethod());
			}
		} else {
			// compatiblity with 0.86 server
			sprite = store.getModifiedSprite(translate("source/" + entityType),
					info.getZoneColor(), info.getColorMethod());
		}

		/*
		 * Entities are [currently] always 1x1. Extra columns are animation.
		 * Extra rows are ignored.
		 */
		final int imageWidth = sprite.getWidth();
		final int width = Math.max((int) entity.getWidth(), 1);
		final int height = Math.max((int) entity.getHeight(), 1);
		int frames = imageWidth / IGameScreen.SIZE_UNIT_PIXELS / width;
		
		// Just use the normal sprite if there are no animation frames
		int state = entity.getState();
		if (frames > 1) {
			sprite = store.getAnimatedSprite(sprite,
					0, state * IGameScreen.SIZE_UNIT_PIXELS * height,
					imageWidth / IGameScreen.SIZE_UNIT_PIXELS / width,
					IGameScreen.SIZE_UNIT_PIXELS * width,
					IGameScreen.SIZE_UNIT_PIXELS * height,
					100);
		} else {
			sprite = store.getTile(sprite, 
					0, state * IGameScreen.SIZE_UNIT_PIXELS * height, 
					IGameScreen.SIZE_UNIT_PIXELS * width,
					IGameScreen.SIZE_UNIT_PIXELS * height);
		}

		setSprite(sprite);
	}
}
