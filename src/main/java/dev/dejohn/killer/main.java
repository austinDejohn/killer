package dev.dejohn.killer;


import dev.dejohn.a.BigBrain;
import dev.dejohn.killer.cards.Card;
import dev.dejohn.killer.cards.Card.Wild;
import dev.dejohn.killer.cards.Cards;
import dev.dejohn.killer.cards.Deck;
import dev.dejohn.killer.cards.Rank;
import dev.dejohn.killer.game.Action;
import dev.dejohn.killer.game.Game;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static dev.dejohn.killer.cards.Rank.*;
import static dev.dejohn.killer.cards.Suit.*;


public class main {
    public static void main(String[] args) {
        var deck = Deck.natural();

        var brains = Set.of(
            new BigBrain("Player 1"),
            new BigBrain("Player 2"),
            new BigBrain("Player 3"),
            new BigBrain("Player 4")
        );

        var game = Game.of(deck, brains);
        game.start();
    }

    private static String formatBits(long bits) {
        return String.format("%64s", Long.toBinaryString(bits)).replace(' ', '0');
    }

    public static void print(long bits) {
        System.out.println(formatBits(bits));
    }

    private static void debugPrint(Cards<? extends Card> cards) {
        System.out.println(
            formatBits(cards.bitVector)
                + " - " + cards.stream().map(Card::toString).collect(Collectors.joining(" "))
                + " - " + cards.size()
                + " - " + cards.getClass().getCanonicalName().replace(cards.getClass().getPackageName() + ".", "")
        );
    }

    private static <T> Action<T> test(Stream<Action<T>> options) {
        return options.findFirst().orElseGet(Action::pass);
    }
}
