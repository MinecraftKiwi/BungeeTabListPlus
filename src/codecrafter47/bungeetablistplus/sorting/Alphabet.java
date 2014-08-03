/*
 * BungeeTabListPlus - a bungeecord plugin to customize the tablist
 *
 * Copyright (C) 2014 Florian Stober
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package codecrafter47.bungeetablistplus.sorting;

import java.text.Collator;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 *
 * @author Florian Stober
 */
public class Alphabet implements ISortingRule {

    @Override
    public int compare(ProxiedPlayer player1, ProxiedPlayer player2) {
        String name1 = player1.getName();
        String name2 = player2.getName();
        return Collator.getInstance().compare(name1, name2);
    }

}
