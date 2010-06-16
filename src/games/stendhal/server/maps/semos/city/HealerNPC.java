package games.stendhal.server.maps.semos.city;

import games.stendhal.server.core.config.ZoneConfigurator;
import games.stendhal.server.core.engine.SingletonRepository;
import games.stendhal.server.core.engine.StendhalRPZone;
import games.stendhal.server.core.pathfinder.FixedPath;
import games.stendhal.server.core.pathfinder.Node;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.npc.behaviour.adder.HealerAdder;
import games.stendhal.server.entity.npc.behaviour.adder.SellerAdder;
import games.stendhal.server.entity.npc.behaviour.impl.SellerBehaviour;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A young lady (original name: Carmen) who heals players without charge. 
 */
public class HealerNPC implements ZoneConfigurator {
	
	public void configureZone(final StendhalRPZone zone,
			final Map<String, String> attributes) {
		buildNPC(zone);
	}

	private void buildNPC(final StendhalRPZone zone) {
		final SpeakerNPC npc = new SpeakerNPC("Carmen") {
			@Override
			public void createDialog() {
				addGreeting("Hi, if I can #help, just say.");
				addJob("My special powers help me to heal wounded people. I also sell potions and antidotes.");
				addHelp("I can #heal you here for free, or you can take one of my prepared medicines with you on your travels; just ask for an #offer.");
				addEmotionReply("hugs", "hugs");
				addGoodbye();
			}

			@Override
			protected void createPath() {
				final List<Node> nodes = new LinkedList<Node>();
				nodes.add(new Node(5, 46));
				nodes.add(new Node(18, 46));
				setPath(new FixedPath(nodes, true));
			}
		};
		new SellerAdder().addSeller(npc, new SellerBehaviour(SingletonRepository.getShopList().get("healing")));
		new HealerAdder().addHealer(npc, 0);
		npc.setPosition(5, 46);
		npc.setEntityClass("welcomernpc");
		zone.add(npc);
	}

}