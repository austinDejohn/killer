package dev.dejohn.killer;

import dev.dejohn.killer.cards.Card;
import dev.dejohn.killer.cards.Cards;

import java.util.stream.Collectors;


public class main {
    public static void main(String[] args) {

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
}
