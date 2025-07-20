package dev.dejohn.killer.game;

import dev.dejohn.killer.Brain;
import dev.dejohn.killer.cards.Cards;
import dev.dejohn.killer.cards.Deck;
import dev.dejohn.killer.cards.Rank;

import java.util.*;
import java.util.stream.Collectors;

import static dev.dejohn.killer.cards.Rank.THREE;
import static dev.dejohn.killer.cards.Suit.CLUBS;

public class Game {
    public static Game of(Deck deck, Set<? extends Brain> brains) {
        return new Game(deck, brains);
    }

    private class Player {
        private final Brain brain;

        private Cards<?> hand = Cards.of();
        private boolean isActive = false;
        private Player next;

        private Player(Brain brain) {
            this.brain = brain;
        }

        private void deal(Cards<?> hand) {
            this.hand = hand;
        }

        private void removeCards(Cards<?> cards) {
            hand = hand.remove(cards);
        }

        private void setActive() {
            isActive = true;
        }

        private boolean isActive() {
            return isActive;
        }

        private boolean isIn() {
            return !hand.isEmpty();
        }

        private boolean isOut() {
            return hand.isEmpty();
        }

        private Player next() {
            return next.isActive ? next : next.next();
        }

        private Context.Opponent asOpponent() {
            return new Context.Opponent(isActive, hand.size());
        }

        private Context context() {
            return new Context(
                deck, pile, hand,
                players.stream().filter(p -> p != this).map(Player::asOpponent)
            );
        }

        private void onAction(Action<?> action) {
            if (action instanceof Action.Play<?> play) {
                var cards = play.select(hand);
                hand = hand.remove(cards);
                toBeat = play;

                if (hand.isEmpty()) isActive = false;
            }
            else isActive = false;

            System.out.println(this + " ---- " + action);
        }

        private void doStartingAction() {
            System.out.println("----------------------------------------");

            onAction(
                brain.getAction(
                    context(),
                    hand.contains(THREE.of(CLUBS))
                        ? Action.stream(hand).filter(a -> a.rank == Rank.THREE)
                        : Action.stream(hand)
                )
            );
        }

        private void doAction() {
            onAction(brain.getAction(context(), toBeat, toBeat.playable(hand)));
        }

        @Override
        public String toString() {
            return brain.toString() + " (" + hand.size() + ")";
        }
    }

    public final Deck deck;
    private final ArrayList<Player> players;

    private Cards<?> pile = Cards.of();
    private Action.Play<?> toBeat = null;
    private Player currentPlayer;

    private Game(Deck deck, Set<? extends Brain> brains) {
        this.deck = deck;
        this.players = brains.stream().map(Player::new).collect(Collectors.toCollection(ArrayList::new));
    }

    private void initialize() {
        pile = Cards.of();
        toBeat = null;
        Collections.shuffle(players);

        var hands = deck.deal(players.size());

        for (int i = 0; i < players.size(); i++) {
            var player = players.get(i);

            if (hands.get(i).contains(THREE.of(CLUBS))) currentPlayer = player;
            player.deal(hands.get(i));
            player.next = (i < players.size() - 1) ? players.get(i + 1) : players.getFirst();
        }
    }

    private boolean isAnyoneActive() {
        return players.stream().anyMatch(Player::isActive);
    }

    private boolean isMoreThanOnePlayerLeft() {
        return players.stream().filter(Player::isIn).limit(2).count() > 1;
    }

    public void start() {
        initialize();
        while (isMoreThanOnePlayerLeft()) newRound();
    }

    private void newRound() {
        players.stream().filter(Player::isIn).forEach(Player::setActive);
        while (currentPlayer.isOut()) currentPlayer = currentPlayer.next();

        currentPlayer.doStartingAction();
        while (isAnyoneActive() && isMoreThanOnePlayerLeft()) (currentPlayer = currentPlayer.next()).doAction();
    }
}
