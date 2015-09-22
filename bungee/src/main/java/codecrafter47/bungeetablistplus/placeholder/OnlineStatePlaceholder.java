/*
 * BungeeTabListPlus - a BungeeCord plugin to customize the tablist
 *
 * Copyright (C) 2014 - 2015 Florian Stober
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

package codecrafter47.bungeetablistplus.placeholder;

import codecrafter47.bungeetablistplus.BungeeTabListPlus;
import codecrafter47.bungeetablistplus.api.PlaceholderProvider;
import codecrafter47.bungeetablistplus.config.MainConfig;
import codecrafter47.bungeetablistplus.managers.PlaceholderManager;
import codecrafter47.bungeetablistplus.tablist.SlotTemplate;
import codecrafter47.util.bungee.PingTask;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Collections;
import java.util.List;

public class OnlineStatePlaceholder extends PlaceholderProvider {
    @Override
    public void setup() {
        Supplier<SlotTemplate> onlineText = Suppliers.memoize(() -> getPlaceholderManager().parseSlot(getMainConfig().online_text));
        Supplier<SlotTemplate> offlineText = Suppliers.memoize(() -> getPlaceholderManager().parseSlot(getMainConfig().offline_text));
        bind("onlineState").withArgs().toTemplate((context, args) -> {
            List<ServerInfo> servers = Lists.transform(context.getServer(), server -> ProxyServer.getInstance().getServerInfo(server));
            if (args != null) {
                ServerInfo server = ProxyServer.getInstance().getServerInfo(args);
                if (server == null) {
                    BungeeTabListPlus.getInstance().getPlugin().getLogger().warning("Server " + args + " does not exist.");
                    return SlotTemplate.text("&c[ERR: \"" + args + "\" not a server]");
                } else {
                    servers = Collections.singletonList(server);
                }
            }
            PingTask ping = BungeeTabListPlus.getInstance().getServerState(
                    servers.get(0).getName());
            if (ping == null) {
                // TODO autostart ping task
                String errorText = "&cPlease set pingDelay in config.yml > 0";
                BungeeTabListPlus.getInstance().getPlugin().getLogger().warning(errorText);
                return SlotTemplate.text(errorText);
            } else {
                return ping.isOnline() ? onlineText.get() : offlineText.get();
            }
        });
    }

    private MainConfig getMainConfig() {
        return BungeeTabListPlus.getInstance().getConfigManager().getMainConfig();
    }

    private PlaceholderManager getPlaceholderManager() {
        return BungeeTabListPlus.getInstance().getPlaceholderManager();
    }
}
