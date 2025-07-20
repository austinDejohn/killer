package dev.dejohn.killer.cards;

import java.util.Arrays;
import java.util.stream.Stream;

public enum Rank {
    THREE("3"), FOUR("4"), FIVE("5"), SIX("6"), SEVEN("7"), EIGHT("8"), NINE("9"),
    TEN("X"), JACK("J"), QUEEN("Q"), KING("K"), ACE("A"), TWO("2");

    public static Stream<Rank> stream() {
        return Arrays.stream(values());
    }

    public final String symbol;
    public final int offset = ordinal() * 4;

    Rank(String symbol) {
        this.symbol = symbol;
    }

    public Card.Natural of(Suit suit) {
        return Card.getNatural(offset + suit.ordinal());
    }

    public Cards<Card.Natural> all() {
        return Cards.all().filter(this);
    }
}
