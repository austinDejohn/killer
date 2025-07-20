package dev.dejohn.killer.game;

import dev.dejohn.killer.cards.Card;
import dev.dejohn.killer.cards.Card.Natural;
import dev.dejohn.killer.cards.Cards;
import dev.dejohn.killer.cards.Rank;
import dev.dejohn.killer.cards.Suit;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static dev.dejohn.killer.cards.Rank.TWO;
import static dev.dejohn.killer.cards.Suit.*;

public sealed abstract class Action<T> {
    public static <T> Pass<T> pass() {
        return new Pass<>();
    }

    public static Stream<Play<Object>> stream(Cards<?> cards) {
        return cards.wilds().stream().findFirst().map(
            w -> Rank.stream().flatMap(
                r -> {
                    long b = ~((cards.bitVector >>> r.offset) & 15);
                    if (b == 0) return Stream.empty();
                    return stream(cards.remove(w).add((b & -b) << r.offset));
                }
            )
        ).orElseGet(
            () -> Stream.iterate(new StreamableRank(cards.naturals()), Objects::nonNull, StreamableRank::next)
                .flatMap(StreamableRank::stream)
        ).distinct();
    }

    public static Single<Object> single(Natural card) {
        if (card == TWO.of(HEARTS)) return new TwoOfHearts<>();
        return new Single<>(card.rank);
    }

    public static Play<Object> of(Rank rank, Depth depth, Length length) {
        return switch (depth) {
            case Depth.SINGLE -> length == Length.ONE ? new Single<>(rank) : new Run<>(rank, length);
            case Depth.PAIR -> length == Length.ONE ? new Pair<>(rank) : new Bomb.Pairs<>(rank, length);
            case Depth.TRIPLE -> length == Length.ONE ? new Triple<>(rank) : new Bomb.Triples<>(rank, length);
            case Depth.QUAD -> {
                if (length != Length.ONE) throw new IllegalArgumentException();
                yield new Bomb.Quad<>(rank);
            }
        };
    }

    public static Stream<Single<Object>> singles(Cards<?> cards) {
        return Stream.concat(
            cards.naturals().stream(),
            cards.wilds().isEmpty() ? Stream.empty() : CLUBS.all().stream()
        ).map(Action::single).distinct();
    }

    public static Stream<Pair<Object>> pairs(Cards<?> cards) {
        var wilds = cards.wilds();

        return switch (wilds.size()) {
            case 0 -> Rank.stream().filter(r -> cards.count(r) >= 2).map(Pair::new);
            case 1 -> cards.naturals().stream().map(Natural::getRank).distinct().map(Pair::new);
            default -> CLUBS.all().stream().map(Natural::getRank).map(Pair::new);
        };
    }

    public static Stream<Triple<Object>> triples(Cards<?> cards) {
        var wilds = cards.wilds();

        return switch (wilds.size()) {
            case 0 -> Rank.stream().filter(r -> cards.count(r) >= 3).map(Triple::new);
            case 1 -> Rank.stream().filter(r -> cards.count(r) >= 2).map(Triple::new);
            case 2 -> cards.naturals().stream().map(Natural::getRank).distinct().map(Triple::new);
            default -> CLUBS.all().stream().map(Natural::getRank).map(Triple::new);
        };
    }

    public static Stream<Bomb.Quad<Object>> quads(Cards<?> cards) {
        var wilds = cards.wilds();

        return switch (wilds.size()) {
            case 0 -> Rank.stream().filter(r -> cards.count(r) == 4).map(Bomb.Quad::new);
            case 1 -> Rank.stream().filter(r -> cards.count(r) >= 3).map(Bomb.Quad::new);
            case 2 -> Rank.stream().filter(r -> cards.count(r) >= 2).map(Bomb.Quad::new);
            case 3 -> cards.naturals().stream().map(Natural::getRank).distinct().map(Bomb.Quad::new);
            default -> CLUBS.all().stream().map(Natural::getRank).map(Bomb.Quad::new);
        };
    }

    public static Stream<Run<Object>> runs(Cards<?> cards, Length length) {
        return anyRun(cards, Depth.SINGLE, length).map(Run.class::cast);
    }

    public static Stream<Bomb.Pairs<Object>> runsOfPairs(Cards<?> cards) {
        return anyRun(cards, Depth.PAIR).map(Bomb.Pairs.class::cast);
    }

    public static Stream<Bomb.Triples<Object>> runsOfTriples(Cards<?> cards) {
        return anyRun(cards, Depth.TRIPLE).map(Bomb.Triples.class::cast);
    }

    public static Stream<Bomb<Object>> bombs(Cards<?> cards) {
        return Stream.of(runsOfPairs(cards), runsOfTriples(cards), quads(cards)).flatMap(s -> s);
    }

    private static Stream<Play<Object>> anyRun(Cards<?> cards, Depth depth, Length length) {
        if (length == Length.ONE || depth == Depth.QUAD) return Stream.empty();

        return Rank.stream().filter(
            rank -> {
                if (rank.ordinal() + length.asInt > Rank.values().length) return false;
                int wildCount = cards.wilds().size();

                for (int i = 0; i < length.asInt; i++) {
                    int missingCount = depth.asInt - cards.count(Rank.values()[rank.ordinal() + i]);

                    if (missingCount > 0) {
                        wildCount -= missingCount;
                        if (wildCount < 0) return false;
                    }
                }

                return true;
            }
        ).map(r -> of(r, depth, length));
    }

    private static Stream<Play<Object>> anyRun(Cards<?> cards, Depth depth) {
        return Length.stream().flatMap(l -> anyRun(cards, depth, l));
    }

    public enum Length {
        ONE(1), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8),
        NINE(9), TEN(10), ELEVEN(11), TWELVE(12), THIRTEEN(13);

        public static Length of(int length) {
            if (length == 1) return ONE;
            if (length >= 3 && length <= 13) return values()[length - 2];
            throw new IllegalArgumentException();
        }

        public static Stream<Length> stream() {
            return Arrays.stream(Length.values());
        }

        public final int asInt;

        Length(int asInt) {
            this.asInt = asInt;
        }
    }

    public enum Depth {
        SINGLE, PAIR, TRIPLE, QUAD;

        public static Depth of(int depth) {
            return Depth.values()[depth - 1];
        }

        public final int asInt;

        Depth() {
            this.asInt = ordinal() + 1;
        }
    }

    private static class StreamableRank {
        private final long bitVector;
        private final Rank rank;
        private final int depthCount;
        private final int[] streaks;

        private StreamableRank(long bitVector, Rank rank, int[] streaks) {
            this.bitVector = bitVector;
            this.rank = rank;
            this.streaks = streaks;

            depthCount = Long.bitCount(bitVector & 15);
        }

        private StreamableRank(Cards<Natural> cards) {
            this(cards.bitVector, Rank.THREE, new int[Depth.QUAD.asInt]);
        }

        private StreamableRank next() {
            int[] nextLengths = new int[Depth.QUAD.asInt];
            for (int d = 0; d < Depth.QUAD.asInt; d++) nextLengths[d] = depthCount > d ? streaks[d] + 1 : 0;

            return rank == TWO ? null
                : new StreamableRank(
                bitVector >>> Depth.QUAD.asInt,
                Rank.values()[rank.ordinal() + 1],
                nextLengths
            );
        }

        private Stream<Play<Object>> stream() {
            return Arrays.stream(Depth.values(), 0, depthCount).flatMap(d -> Stream.concat(nonRuns(d), runs(d)));
        }

        private Stream<Play<Object>> nonRuns(Depth depth) {
            return switch (depth) {
                case Depth.PAIR -> Stream.of(new Pair<>(rank));
                case Depth.TRIPLE -> Stream.of(new Triple<>(rank));
                case Depth.QUAD -> Stream.of(new Bomb.Quad<>(rank));
                case Depth.SINGLE -> {
                    if (rank == Rank.TWO && (bitVector & (1 << HEARTS.ordinal())) != 0) {
                        yield depthCount == 1
                            ? Stream.of(new TwoOfHearts<>())
                            : Stream.of(new Single<>(rank), new TwoOfHearts<>());
                    }

                    yield Stream.of(new Single<>(rank));
                }
            };
        }

        private Stream<Play<Object>> runs(Depth depth) {
            if (depth == Depth.QUAD) return Stream.empty();

            return IntStream.rangeClosed(Length.THREE.asInt, streaks[depth.ordinal()] + 1).mapToObj(Length::of)
                .map(l -> Action.of(Rank.values()[rank.ordinal() - l.asInt + 1], depth, l));
        }
    }

    public static final class Pass<T> extends Action<T> {
        @Override
        public String toString() {
            return "PASS";
        }
    }

    public static sealed abstract class Play<T> extends Action<T> {
        public final Rank rank;
        public final Depth depth;
        public final Length length;

        private Play(Rank rank, Depth depth, Length length) {
            this.rank = rank;
            this.depth = depth;
            this.length = length;
        }

        abstract public boolean yieldsTo(Play<?> play);
        abstract public Stream<? extends Play<Object>> playable(Cards<?> cards);

        public Cards<?> select(Cards<?> hand) {
            var cards = Cards.from(
                Arrays.stream(Rank.values(), rank.ordinal(), rank.ordinal() + length.asInt)
                    .map(hand::filter)
                    .flatMapToLong(rankCards -> rankCards.bitStream().limit(depth.asInt))
                    .reduce(0L, (a,b) -> a|b)
            );

            int missingCount = size() - cards.size();
            if (missingCount == 0) return cards;

            var wilds = hand.wilds();
            if (missingCount > wilds.size()) return Cards.of();

            return cards.add(
                wilds.bitStream().limit(missingCount).reduce(0L, (a,b) -> a|b)
            );
        }

        public int size() {
            return depth.asInt * length.asInt;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Play<?> p
                && rank == p.rank
                && depth == p.depth
                && length == p.length;
        }

        @Override
        public int hashCode() {
            return rank.ordinal() + (depth.ordinal() << 4) + (length.ordinal() << 6);
        }
    }

    public static sealed class Single<T> extends Play<T> {
        private Single(Rank rank) {
            super(rank, Depth.SINGLE, Length.ONE);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Single<?> s && rank == s.rank;
        }

        @Override
        public boolean yieldsTo(Play<?> play) {
            return switch (play) {
                case TwoOfHearts<?> ignore -> true;
                case Bomb<?> ignore -> true;
                case Single<?> s -> rank.compareTo(s.rank) < 1;
                default -> false;
            };
        }

        @Override
        public Stream<? extends Play<Object>> playable(Cards<?> cards) {
            return Stream.concat(singles(cards).filter(this::yieldsTo), bombs(cards));
        }

        @Override
        public Cards<?> select(Cards<?> hand) {
            var cards = Cards.from(hand.filter(rank).bitStream().findFirst().orElse(0));

            return (cards.isEmpty() || cards.contains(TWO.of(HEARTS)))
                ? Cards.from(hand.wilds().bitStream().findFirst().orElse(0))
                : cards;
        }

        @Override
        public String toString() {
            return "SINGLE (" + rank.symbol + ")";
        }
    }

    public static final class TwoOfHearts<T> extends Single<T> {
        private TwoOfHearts() {
            super(TWO);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Action.TwoOfHearts;
        }

        @Override
        public int hashCode() {
            return super.hashCode() + 1;
        }

        @Override
        public boolean yieldsTo(Play<?> play) {
            return play instanceof Bomb;
        }

        @Override
        public Stream<? extends Play<Object>> playable(Cards<?> cards) {
            return bombs(cards);
        }

        @Override
        public Cards<?> select(Cards<?> hand) {
            return hand.contains(TWO.of(HEARTS)) ? Cards.of(TWO.of(HEARTS)) : Cards.of();
        }

        @Override
        public String toString() {
            return "TWO of HEARTS";
        }
    }

    public static final class Pair<T> extends Play<T> {
        private Pair(Rank rank) {
            super(rank, Depth.PAIR, Length.ONE);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Pair<?> p && rank == p.rank;
        }

        @Override
        public boolean yieldsTo(Play<?> play) {
            return play instanceof Bomb || (play instanceof Pair<?> p && rank.compareTo(p.rank) < 1);
        }

        @Override
        public Stream<? extends Play<Object>> playable(Cards<?> cards) {
            return Stream.concat(pairs(cards).filter(this::yieldsTo), bombs(cards));
        }

        @Override
        public String toString() {
            return "PAIR (" + rank.symbol + ")";
        }
    }

    public static final class Triple<T> extends Play<T> {
        private Triple(Rank rank) {
            super(rank, Depth.TRIPLE, Length.ONE);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Triple<?> t && rank == t.rank;
        }

        @Override
        public boolean yieldsTo(Play<?> play) {
            return play instanceof Bomb || (play instanceof Triple<?> t && rank.compareTo(t.rank) < 1);
        }

        @Override
        public Stream<? extends Play<Object>> playable(Cards<?> cards) {
            return Stream.concat(triples(cards).filter(this::yieldsTo), bombs(cards));
        }

        @Override
        public String toString() {
            return "TRIPLE (" + rank.symbol + ")";
        }
    }

    public static final class Run<T> extends Play<T> {
        private Run(Rank rank, Length length) {
            super(rank, Depth.SINGLE, length);
        }

        @Override
        public String toString() {
            return "RUN of " + length.name() + " (" + rank.symbol + ")";
        }

        @Override
        public boolean yieldsTo(Play<?> play) {
            return play instanceof Bomb
                || (play instanceof Run<?> r && length == r.length && rank.compareTo(r.rank) < 1);
        }

        @Override
        public Stream<? extends Play<Object>> playable(Cards<?> cards) {
            return Stream.concat(runs(cards, length).filter(this::yieldsTo), bombs(cards));
        }
    }

    public abstract sealed static class Bomb<T> extends Play<T> {
        public static final class Pairs<T> extends Bomb<T> implements Comparable<Pairs<?>> {
            private Pairs(Rank rank, Length length) {
                super(rank, Depth.PAIR, length);
            }

            @Override
            public String toString() {
                return "PAIRS BOMB of " + length.name() + " (" + rank.symbol + ")";
            }

            @Override
            public int compareTo(Pairs<?> r) {
                return length == r.length ? rank.compareTo(r.rank) : length.compareTo(r.length);
            }

            @Override
            public boolean yieldsTo(Play<?> play) {
                return switch (play) {
                    case Pairs<?> r -> compareTo(r) < 1;
                    case Triples<?> ignore -> true;
                    case Quad<?> ignore -> true;
                    default -> false;
                };
            }

            @Override
            public Stream<? extends Play<Object>> playable(Cards<?> cards) {
                return bombs(cards).filter(this::yieldsTo);
            }
        }

        public static final class Triples<T> extends Bomb<T> implements Comparable<Triples<?>> {
            private Triples(Rank rank, Length length) {
                super(rank, Depth.TRIPLE, length);
            }

            @Override
            public String toString() {
                return "TRIPLES BOMB of " + length.name() + " (" + rank.symbol + ")";
            }

            @Override
            public int compareTo(Triples<?> r) {
                return length == r.length ? rank.compareTo(r.rank) : length.compareTo(r.length);
            }

            @Override
            public boolean yieldsTo(Play<?> play) {
                return switch (play) {
                    case Triples<?> r -> compareTo(r) < 1;
                    case Quad<?> ignore -> true;
                    default -> false;
                };
            }

            @Override
            public Stream<? extends Play<Object>> playable(Cards<?> cards) {
                return Stream.concat(runsOfTriples(cards), quads(cards)).filter(this::yieldsTo);
            }
        }

        public static final class Quad<T> extends Bomb<T> implements Comparable<Quad<?>> {
            private Quad(Rank rank) {
                super(rank, Depth.QUAD, Length.ONE);
            }

            @Override
            public String toString() {
                return "QUAD BOMB (" + rank.symbol + ")";
            }

            @Override
            public int compareTo(Quad<?> q) {
                return rank.compareTo(q.rank);
            }

            @Override
            public boolean yieldsTo(Play<?> play) {
                return play instanceof Quad<?> q && compareTo(q) < 1;
            }

            @Override
            public Stream<? extends Play<Object>> playable(Cards<?> cards) {
                return quads(cards).filter(this::yieldsTo);
            }
        }

        private Bomb(Rank rank, Depth depth, Length length) {
            super(rank, depth, length);
        }
    }
}
