package com.aircandi.applications;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.IntentBuilder;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ExcludeNulls;
import com.aircandi.service.HttpService.UseAnnotations;
import com.aircandi.service.objects.AirMarker;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.MapForm;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;

public class Maps {

	public static int	ICON_COLOR	= R.color.accent_blue_dark;

	public static void view(Context context, Entity entity) {

		if (entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {

			Bundle extras = new Bundle();
			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CANDIGRAM, Constants.SCHEMA_ENTITY_PLACE, Direction.out, false, false);
			settings.appClass = Places.class;
			List<Shortcut> shortcuts = (List<Shortcut>) entity.getShortcuts(settings, new Link.SortByModifiedDate());
			if (shortcuts.size() > 0) {
				final List<String> markerStrings = new ArrayList<String>();
				for (Shortcut shortcut : shortcuts) {
					if (shortcut.location != null) {
						AirMarker marker = new AirMarker(shortcut.name, null, shortcut.location.lat, shortcut.location.lng, false,
								R.drawable.img_marker_candigram_inactive);
						if (!shortcut.inactive) {
							marker.current = true;
							marker.iconResId = R.drawable.img_marker_candigram;
						}
						markerStrings.add(HttpService.objectToJson(marker, UseAnnotations.False, ExcludeNulls.True));
					}
				}
				if (markerStrings.size() > 0) {
					extras.putStringArrayList(Constants.EXTRA_MARKERS, (ArrayList<String>) markerStrings);
				}
			}

			IntentBuilder intentBuilder = new IntentBuilder(context, MapForm.class)
					.setEntityId(entity.id)
					.setExtras(extras);
			context.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition((Activity) context, TransitionType.PageToPage);
		}
		else {
			IntentBuilder intentBuilder = new IntentBuilder(context, MapForm.class).setEntityId(entity.id);
			context.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition((Activity) context, TransitionType.PageToPage);
		}
	}
}