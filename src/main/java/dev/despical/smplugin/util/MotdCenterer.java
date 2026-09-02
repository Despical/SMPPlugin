/*
 * SMPlugin - A utility plugin for Minecraft servers.
 * Copyright (C) 2026  Berke Akçen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dev.despical.smplugin.util;

import dev.despical.commons.miscellaneous.DefaultFontInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * @author Despical
 * <p>
 * Created at 17.08.2026
 */
public final class MotdCenterer {

    private MotdCenterer() {
    }

    public static Component center(Component component, int lineWidth) {
        int messageWidth = measure(component, false);
        int remainingWidth = Math.max(0, lineWidth - messageWidth);
        int spaceWidth = DefaultFontInfo.SPACE.getLength() + 1;
        int paddingSpaces = remainingWidth / 2 / spaceWidth;

        return Component.text(" ".repeat(paddingSpaces)).append(component);
    }

    private static int measure(Component component, boolean inheritedBold) {
        boolean bold = switch (component.style().decoration(TextDecoration.BOLD)) {
            case TRUE -> true;
            case FALSE -> false;
            case NOT_SET -> inheritedBold;
        };

        int width = 0;

        if (component instanceof TextComponent textComponent) {
            for (int index = 0; index < textComponent.content().length(); index++) {
                DefaultFontInfo fontInfo = DefaultFontInfo.getDefaultFontInfo(textComponent.content().charAt(index));
                width += bold ? fontInfo.getBoldLength() : fontInfo.getLength();
                width++;
            }
        }

        for (Component child : component.children()) {
            width += measure(child, bold);
        }

        return width;
    }
}
