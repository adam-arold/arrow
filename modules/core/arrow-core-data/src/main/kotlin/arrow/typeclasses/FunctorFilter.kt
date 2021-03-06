package arrow.typeclasses

import arrow.Kind
import arrow.core.None
import arrow.core.Option
import arrow.core.PartialFunction
import arrow.core.Some
import arrow.core.identity
import arrow.core.lift

/**
 * ank_macro_hierarchy(arrow.typeclasses.FunctorFilter)
 *
 * A Functor with the ability to [mapFilter].
 * Enables [collect] based on [PartialFunction] predicates.
 */
interface FunctorFilter<F> : Functor<F> {

  /**
   * A combined map and filter. Filtering is handled via Option instead of Boolean such that the output type B can be different than the input type A.
   */
  fun <A, B> Kind<F, A>.mapFilter(f: (A) -> Option<B>): Kind<F, B>

  /**
   * Similar to mapFilter but uses a partial function instead of a function that returns an Option.
   */
  @Deprecated("PartialFunction is an incomplete experiment due for removal. See https://github.com/arrow-kt/arrow/pull/1419#issue-273308228")
  fun <A, B> Kind<F, A>.collect(f: PartialFunction<A, B>): Kind<F, B> =
    mapFilter(f.lift())

  /**
   * "Flatten" out a structure by collapsing Options.
   */
  fun <A> Kind<F, Option<A>>.flattenOption(): Kind<F, A> = mapFilter(::identity)

  /**
   * Apply a filter to a structure such that the output structure contains all A elements in the input structure that satisfy the predicate f but none
   * that don't.
   */
  fun <A> Kind<F, A>.filter(f: (A) -> Boolean): Kind<F, A> =
    mapFilter { a -> if (f(a)) Some(a) else None }
}
