package net.scrumplex.waitingfor;

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClientInfo;
import net.scrumplex.sprummlbot.plugins.SprummlbotPlugin;
import net.scrumplex.sprummlbot.plugins.events.ClientJoinEventHandler;
import net.scrumplex.sprummlbot.plugins.events.ClientQuitEventHandler;
import net.scrumplex.sprummlbot.wrapper.ChatCommand;
import net.scrumplex.sprummlbot.wrapper.CommandResponse;
import java.util.HashMap;
import java.util.Map;

public class Main extends SprummlbotPlugin {

    private Map<Integer, String> watcherList = new HashMap<>();
    private TS3ApiAsync api;

    @Override
    public void onEnable() {
        api = getSprummlbot().getDefaultAPI();
        registerCommands();
        registerEvents();
    }

    private void registerCommands() {
        ChatCommand cmdWaitingFor = getSprummlbot().getCommandManager().registerCommand("waitingfor", "!waitingfor [name/uid]");
        cmdWaitingFor.setCommandHandler(this, (c, args) -> {
            if (args.length > 0) {
                final String arg0 = args[0];
                if (arg0.endsWith("=") && arg0.length() == 28) {
                    api.getDatabaseClientByUId(arg0).onSuccess(databaseClientInfo -> {
                        watcherList.put(c.getId(), databaseClientInfo.getUniqueIdentifier());
                        api.sendPrivateMessage(c.getId(), databaseClientInfo.getNickname() + " was added to the waiting list!");
                    });
                    return CommandResponse.SUCCESS;
                } else {
                    api.getDatabaseClientsByName(arg0).onSuccess(databaseClientInfos -> {
                        if (databaseClientInfos.size() == 0) {
                            api.sendPrivateMessage(c.getId(), "Nothing found for " + arg0 + "!");
                        } else if (databaseClientInfos.size() == 1) {
                            DatabaseClientInfo databaseClientInfo = databaseClientInfos.get(0);
                            watcherList.put(c.getId(), databaseClientInfo.getUniqueIdentifier());
                            api.sendPrivateMessage(c.getId(), databaseClientInfo.getNickname() + " was added to the waiting list!");
                        } else {
                            api.sendPrivateMessage(c.getId(), "There were too many results in the database! " + arg0 + " was not added to watching list!");
                        }
                    });
                    return CommandResponse.SUCCESS;
                }
            }
            return CommandResponse.SYNTAX_ERROR;
        });
    }

    private void registerEvents() {
        getEventManager().addEventListener((ClientJoinEventHandler) e -> {
            if (watcherList.containsValue(e.getUniqueClientIdentifier())) {
                for (Integer clientId : watcherList.keySet()) {
                    if (watcherList.get(clientId).equals(e.getUniqueClientIdentifier())) {
                        api.pokeClient(clientId, e.getClientNickname() + " joined the server!");
                        watcherList.remove(clientId);
                    }
                }
            }
        });
        getEventManager().addEventListener((ClientQuitEventHandler) clientLeaveEvent -> {
            if (watcherList.containsKey(clientLeaveEvent.getClientId())) {
                watcherList.remove(clientLeaveEvent.getClientId());
            }
        });
    }
}
