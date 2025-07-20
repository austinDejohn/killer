package dev.dejohn.killer.cards;

import java.util.Arrays;

public enum Suit {
    CLUBS("♣"), SPADES("♠"), DIAMONDS("♦"), HEARTS("♥");

    public final String symbol;

    Suit(String symbol) {
        this.symbol = symbol;
    }

    public Cards<Card.Natural> all() {
        return Cards.all().filter(this);
    }
}
