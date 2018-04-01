/*
 * Java Genetic Algorithm Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
 */
package io.jenetics.util;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import io.jenetics.internal.collection.Array;
import io.jenetics.internal.collection.ArrayMSeq;
import io.jenetics.internal.collection.Empty;
import io.jenetics.internal.collection.Empty.EmptyMSeq;
import io.jenetics.internal.collection.ObjectStore;

/**
 * Mutable, ordered, fixed sized sequence.
 *
 * @implNote
 * This implementation is not thread safe. All {@link ISeq} and {@link MSeq}
 * instances created by {@link MSeq#toISeq} and {@link MSeq#subSeq(int)},
 * respectively, must be protected by the same lock, when they are accessed
 * (get/set) by different threads.
 *
 * @see ISeq
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @since 1.0
 * @version 3.4
 */
public interface MSeq<T> extends Seq<T>, Copyable<MSeq<T>> {

	public default List<T> asList() {
		return new MSeqList<>(this);
	}

	/**
	 * Set the {@code value} at the given {@code index}.
	 *
	 * @param index the index of the new value.
	 * @param value the new value.
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *         {@code (index < 0 || index >= size())}.
	 */
	public void set(final int index, final T value);

	/**
	 * Fills the sequence with values of the given iterator.
	 *
	 * @param it the iterator of the values to fill this sequence.
	 * @return {@code this} sequence.
	 */
	public default MSeq<T> setAll(final Iterator<? extends T> it) {
		for (int i = 0, n = length(); i < n && it.hasNext(); ++i) {
			set(i, it.next());
		}
		return this;
	}

	/**
	 * Fills the sequence with values of the given iterable.
	 *
	 * @param values the values to fill this sequence.
	 * @return {@code this} sequence.
	 */
	public default MSeq<T> setAll(final Iterable<? extends T> values) {
		setAll(values.iterator());
		return this;
	}

	/**
	 * Fill the sequence with the given values.
	 *
	 * @param values the first initial values of this sequence
	 * @return {@code this} sequence.
	 */
	public default MSeq<T> setAll(final T[] values) {
		for (int i = 0, n = min(length(), values.length); i < n; ++i) {
			set(i, values[i]);
		}
		return this;
	}

	/**
	 * Fill the sequence with values generated by the given factory.
	 *
	 * @param supplier the value factory.
	 * @return {@code this} sequence.
	 * @throws NullPointerException if the given {@code factory} is {@code null}.
	 */
	public default MSeq<T> fill(final Supplier<? extends T> supplier) {
		for (int i = 0, n = length(); i < n; ++i) {
			set(i, supplier.get());
		}
		return this;
	}

	/**
	 * Swap the elements at the two positions.
	 *
	 * @param i the index of the first element.
	 * @param j the index of the second element.
	 * @throws IndexOutOfBoundsException if {@code i < 0 || j >= length()}.
	 */
	public default void swap(final int i, final int j) {
		final T temp = get(i);
		set(i, get(j));
		set(j, temp);
	}

	/**
	 * Swap a given range with a range of the same size with another array.
	 *
	 * <pre>
	 *            start                end
	 *              |                   |
	 * this:  +---+---+---+---+---+---+---+---+---+---+---+---+
	 *              +---------------+
	 *                          +---------------+
	 * other: +---+---+---+---+---+---+---+---+---+---+---+---+
	 *                          |
	 *                      otherStart
	 * </pre>
	 *
	 * @param start the start index of {@code this} range, inclusively.
	 * @param end the end index of {@code this} range, exclusively.
	 * @param other the other array to swap the elements with.
	 * @param otherStart the start index of the {@code other} array.
	 * @throws IndexOutOfBoundsException if {@code start > end} or
	 *         if {@code start < 0 || end >= this.length() || otherStart < 0 ||
	 *         otherStart + (end - start) >= other.length()}
	 */
	public default void swap(
		final int start, final int end,
		final MSeq<T> other, final int otherStart
	) {
		if (otherStart < 0 || (otherStart + (end - start)) > length()) {
			throw new ArrayIndexOutOfBoundsException(format(
				"Invalid index range: [%d, %d)",
				otherStart, otherStart + (end - start)
			));
		}

		if (start < end) {
			for (int i = end - start; --i >= 0;) {
				final T temp = get(start + i);
				set(start + i, other.get(otherStart + i));
				other.set(otherStart + i, temp);
			}
		}
	}

	/**
	 * Swap the elements at the same position.
	 *
	 * @since 4.0
	 *
	 * @param index the index of swapped element.
	 * @param other the other array to swap the elements with.
	 * @throws IndexOutOfBoundsException if
	 *        {@code index < 0 || index >= this.length() || index >= other.length()}.
	 * @throws NullPointerException if the {@code other} sequence is {@code null}
	 */
	default void swap(final int index, final MSeq<T> other) {
		final T temp = get(index);
		set(index, other.get(index));
		other.set(index, temp);
	}

	/**
	 * Randomize the {@code array} using the {@link Random} object currently
	 * registered in the {@link RandomRegistry} class. The used shuffling
	 * algorithm is from D. Knuth TAOCP, Seminumerical Algorithms, Third edition,
	 * page 142, Algorithm S (Selection sampling technique).
	 *
	 * @return this shuffled sequence
	 */
	public default MSeq<T> shuffle() {
		return shuffle(RandomRegistry.getRandom());
	}

	/**
	 * Randomize the {@code array} using the given {@link Random} object. The used
	 * shuffling algorithm is from D. Knuth TAOCP, Seminumerical Algorithms,
	 * Third edition, page 142, Algorithm S (Selection sampling technique).
	 *
	 * @param random the {@link Random} object to use for randomize.
	 * @return this shuffled sequence
	 * @throws NullPointerException if the random object is {@code null}.
	 */
	public default MSeq<T> shuffle(final Random random) {
		for (int j = length() - 1; j > 0; --j) {
			swap(j, random.nextInt(j + 1));
		}
		return this;
	}

	/**
	 * Sorts this sequence according to the order induced by the specified
	 * {@link Comparator}.
	 *
	 * <p>All elements in this sequence must be <i>mutually comparable</i> using
	 * the specified comparator (that is, {@code c.compare(e1, e2)} must not
	 * throw a {@code ClassCastException} for any elements {@code e1} and
	 * {@code e2} in the sequence).
	 *
	 * <p>If the specified comparator is {@code null} then all elements in this
	 * list must implement the {@link Comparable} interface and the elements'
	 * Comparable natural ordering should be used.
	 *
	 * @param start the start index where to start sorting (inclusively)
	 * @param end the end index where to stop sorting (exclusively)
	 * @param comparator the {@code Comparator} used to compare sequence elements.
	 *          A {@code null} value indicates that the elements' Comparable
	 *          natural ordering should be used
	 * @throws ClassCastException if the sequence contains elements that are not
	 *         <i>mutually comparable</i> using the specified comparator
	 * @return {@code this} sequence
	 */
	public MSeq<T> sort(
		final int start,
		final int end,
		final Comparator<? super T> comparator
	);

	/**
	 * Sorts this sequence according to the natural order of the elements.
	 *
	 * @param start the start index where to start sorting (inclusively)
	 * @param end the end index where to stop sorting (exclusively)
	 * @throws ClassCastException if the sequence contains elements that are not
	 *         <i>mutually comparable</i> using the specified comparator
	 * @return {@code this} sequence
	 */
	public default MSeq<T> sort(final int start, final int end) {
		return sort(start, end, null);
	}

	/**
	 * Sorts this sequence according to the order induced by the specified
	 * {@link Comparator}.
	 *
	 * <p>All elements in this sequence must be <i>mutually comparable</i> using
	 * the specified comparator (that is, {@code c.compare(e1, e2)} must not
	 * throw a {@code ClassCastException} for any elements {@code e1} and
	 * {@code e2} in the sequence).
	 *
	 * <p>If the specified comparator is {@code null} then all elements in this
	 * list must implement the {@link Comparable} interface and the elements'
	 * Comparable natural ordering should be used.
	 *
	 * @param start the start index where to start sorting (inclusively)
	 * @param comparator the {@code Comparator} used to compare sequence elements.
	 *          A {@code null} value indicates that the elements' Comparable
	 *          natural ordering should be used
	 * @throws ClassCastException if the sequence contains elements that are not
	 *         <i>mutually comparable</i> using the specified comparator
	 * @return {@code this} sequence
	 */
	public default MSeq<T> sort(
		final int start,
		final Comparator<? super T> comparator
	) {
		return sort(start, length(), comparator);
	}

	/**
	 * Sorts this sequence according to the natural order of the elements.
	 *
	 * @param start the start index where to start sorting (inclusively)
	 * @throws ClassCastException if the sequence contains elements that are not
	 *         <i>mutually comparable</i> using the specified comparator
	 * @return {@code this} sequence
	 */
	public default MSeq<T> sort(final int start) {
		return sort(start, length(), null);
	}

	/**
	 * Sorts this sequence according to the order induced by the specified
	 * {@link Comparator}.
	 *
	 * <p>All elements in this sequence must be <i>mutually comparable</i> using
	 * the specified comparator (that is, {@code c.compare(e1, e2)} must not
	 * throw a {@code ClassCastException} for any elements {@code e1} and
	 * {@code e2} in the sequence).
	 *
	 * <p>If the specified comparator is {@code null} then all elements in this
	 * list must implement the {@link Comparable} interface and the elements'
	 * Comparable natural ordering should be used.
	 *
	 * @param comparator the {@code Comparator} used to compare sequence elements.
	 *          A {@code null} value indicates that the elements' Comparable
	 *          natural ordering should be used
	 * @throws ClassCastException if the sequence contains elements that are not
	 *         <i>mutually comparable</i> using the specified comparator
	 * @return {@code this} sequence
	 */
	public default MSeq<T> sort(final Comparator<? super T> comparator) {
		return sort(0, length(), comparator);
	}

	/**
	 * Sorts this sequence according to the natural order of the elements.
	 *
	 * @throws ClassCastException if the sequence contains elements that are not
	 *         <i>mutually comparable</i> using the specified comparator
	 * @return {@code this} sequence
	 */
	public default MSeq<T> sort() {
		return sort(0, length(), null);
	}

	/**
	 * Reverses the order of the elements this sequence (in place).
	 *
	 * @return this sequence with reverse order or the elements
	 */
	public default MSeq<T> reverse() {
		for (int i = 0, j = length() - 1; i < j; ++i, --j) {
			swap(i, j);
		}
		return this;
	}

	/**
	 * Returns a list iterator over the elements in this sequence (in proper
	 * sequence).
	 *
	 * @return a list iterator over the elements in this list (in proper
	 *         sequence)
	 */
	public default ListIterator<T> listIterator() {
		return asList().listIterator();
	}

	@Override
	public MSeq<T> subSeq(final int start, final int end);

	@Override
	public MSeq<T> subSeq(final int start);

	@Override
	public <B> MSeq<B> map(final Function<? super T, ? extends B> mapper);

	@SuppressWarnings("unchecked")
	@Override
	public default MSeq<T> append(final T... values) {
		return append(MSeq.of(values));
	}

	@Override
	public MSeq<T> append(final Iterable<? extends T> values);

	@SuppressWarnings("unchecked")
	@Override
	public default MSeq<T> prepend(final T... values) {
		return prepend(MSeq.of(values));
	}

	@Override
	public MSeq<T> prepend(final Iterable<? extends T> values);

	/**
	 * Return a read-only projection of this sequence. Changes to the original
	 * sequence will not influence the returned {@code ISeq}.
	 *
	 * @return a read-only projection of this sequence
	 */
	public ISeq<T> toISeq();


	/* *************************************************************************
	 *  Some static factory methods.
	 * ************************************************************************/

	/**
	 * Single instance of an empty {@code MSeq}.
	 */
	public static final MSeq<?> EMPTY = EmptyMSeq.INSTANCE;

	/**
	 * Return an empty {@code MSeq}.
	 *
	 * @param <T> the element type of the returned {@code MSeq}.
	 * @return an empty {@code MSeq}.
	 */
	public static <T> MSeq<T> empty() {
		return Empty.mseq();
	}

	/**
	 * Returns a {@code Collector} that accumulates the input elements into a
	 * new {@code MSeq}.
	 *
	 * @param <T> the type of the input elements
	 * @return a {@code Collector} which collects all the input elements into a
	 *         {@code MSeq}, in encounter order
	 */
	public static <T> Collector<T, ?, MSeq<T>> toMSeq() {
		return Collector.of(
			(Supplier<List<T>>)ArrayList::new,
			List::add,
			(left, right) -> { left.addAll(right); return left; },
			MSeq::of
		);
	}

	/**
	 * Create a new {@code MSeq} with the given {@code length}.
	 *
	 * @param length the length of the created {@code MSeq}.
	 * @param <T> the element type of the new {@code MSeq}.
	 * @return the new mutable sequence.
	 * @throws NegativeArraySizeException if the given {@code length} is
	 *         negative
	 */
	public static <T> MSeq<T> ofLength(final int length) {
		return length == 0
			? empty()
			: new ArrayMSeq<>(Array.of(ObjectStore.ofLength(length)));
	}

	/**
	 * Create a new {@code MSeq} from the given values.
	 *
	 * @param <T> the element type
	 * @param values the array values.
	 * @return a new {@code Meq} with the given values.
	 * @throws NullPointerException if the {@code values} array is {@code null}.
	 */
	@SafeVarargs
	public static <T> MSeq<T> of(final T... values) {
		return values.length == 0
			? empty()
			: new ArrayMSeq<>(Array.of(ObjectStore.of(values.clone())));
	}

	/**
	 * Create a new {@code MSeq} from the given values.
	 *
	 * @param <T> the element type
	 * @param values the array values.
	 * @return a new {@code MSeq} with the given values.
	 * @throws NullPointerException if the {@code values} object is
	 *        {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public static <T> MSeq<T> of(final Iterable<? extends T> values) {
		final MSeq<T> mseq;
		if (values instanceof ISeq<?>) {
			final ISeq<T> seq = (ISeq<T>)values;
			mseq = seq.isEmpty() ? empty() : seq.copy();
		} else if (values instanceof MSeq<?>) {
			final MSeq<T> seq = (MSeq<T>)values;
			mseq = seq.isEmpty() ? empty() : MSeq.of(seq);
		} else if (values instanceof Collection<?>) {
			final Collection<T> collection = (Collection<T>)values;
			mseq = collection.isEmpty()
				? empty()
				: MSeq.<T>ofLength(collection.size()).setAll(values);
		} else {
			final Stream.Builder<T> builder = Stream.builder();
			values.forEach(builder::add);
			final Object[] objects = builder.build().toArray();

			mseq = objects.length == 0
				? empty()
				: new ArrayMSeq<>(Array.of(ObjectStore.of(objects)));
		}

		return mseq;
	}

	public static <T> MSeq<T> viewOf(final List<? extends T> list) {
		return null;
	}

//	/**
//	 * Create a new {@code MSeq} instance from the remaining elements of the
//	 * given iterator.
//	 *
//	 * @since 3.3
//	 *
//	 * @param <T> the element type.
//	 * @return a new {@code MSeq} with the given remaining values.
//	 * @throws NullPointerException if the {@code values} object is
//	 *        {@code null}.
//	 */
//	public static <T> MSeq<T> of(final Iterator<? extends T> values) {
//		final Stream.Builder<T> builder = Stream.builder();
//		values.forEachRemaining(builder::add);
//		final Object[] objects = builder.build().toArray();
//
//		return objects.length == 0
//			? empty()
//			: new ArrayProxyMSeq<>(
//				new ObjectArrayProxy<>(objects, 0, objects.length));
//	}

	/**
	 * Creates a new sequence, which is filled with objects created be the given
	 * {@code supplier}.
	 *
	 * @since 3.3
	 *
	 * @param <T> the element type of the sequence
	 * @param supplier the {@code Supplier} which creates the elements, the
	 *        returned sequence is filled with
	 * @param length the length of the returned sequence
	 * @return a new sequence filled with elements given by the {@code supplier}
	 * @throws NegativeArraySizeException if the given {@code length} is
	 *         negative
	 * @throws NullPointerException if the given {@code supplier} is
	 *         {@code null}
	 */
	public static <T> MSeq<T> of(
		final Supplier<? extends T> supplier,
		final int length
	) {
		requireNonNull(supplier);

		return length == 0
			? empty()
			: MSeq.<T>ofLength(length).fill(supplier);
	}

	/**
	 * Create a new {@code MSeq} from the values of the given {@code Seq}.
	 *
	 * @param <T> the element type
	 * @param values the array values.
	 * @return an new {@code MSeq} with the given values
	 * @throws NullPointerException if the {@code values} array is {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public static <T> MSeq<T> of(final Seq<? extends T> values) {
		final MSeq<T> result;
		if (values instanceof MSeq<?>) {
			result = ((MSeq<T>)values).copy();
		} else if (values instanceof ISeq<?>) {
			result = ((ISeq<T>)values).copy();
		} else {
			result = MSeq.<T>ofLength(values.length()).setAll(values);
		}
		return result;
	}

}
