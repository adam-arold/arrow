package arrow.effects.internal

import arrow.Kind
import arrow.effects.typeclasses.Concurrent
import arrow.effects.typeclasses.ExitCase
import kotlin.coroutines.CoroutineContext

fun <F, A, B, C, D> Concurrent<F>.parMap3(ctx: CoroutineContext, fa: Kind<F, A>, fb: Kind<F, B>, fc: Kind<F, C>, f: (A, B, C) -> D): Kind<F, D> = ctx.run {
  tupled(startFiber(fa), startFiber(fb), startFiber(fc)).bracketCase(use = { (fiberA, fiberB, fiberC) ->
    raceTriple(fiberA.join().attempt(), fiberB.join().attempt(), fiberC.join().attempt()).flatMap { tripleResult ->
      tripleResult.fold({ attemptedA, fiberB, fiberC ->
        attemptedA.fold({ error ->
          raiseError<D>(error)
        }, { a ->
          racePair(fiberB.join(), fiberC.join()).flatMap {
            it.fold({ attemptedB, fiberC ->
              attemptedB.fold({ error ->
                raiseError<D>(error)
              }, { b ->
                fiberC.join().rethrow().map { c ->
                  f(a, b, c)
                }
              })
            }, { fiberB, c ->
              c.fold({ error ->
                raiseError(error)
              }, { c ->
                fiberB.join().rethrow().map { b ->
                  f(a, b, c)
                }
              })
            })
          }
        })
      }, { fiberA, attemptedB, fiberC ->
        attemptedB.fold({ error ->
          raiseError<D>(error)
        }, { b ->
          racePair(fiberA.join(), fiberC.join()).flatMap {
            it.fold({ attemptedA, fiberC ->
              attemptedA.fold({ error ->
                raiseError<D>(error)
              }, { a ->
                fiberC.join().rethrow().map { c ->
                  f(a, b, c)
                }
              })
            }, { fiberA, attemptedC ->
              attemptedC.fold({ error ->
                raiseError<D>(error)
              }, { c ->
                fiberA.join().rethrow().map { a ->
                  f(a, b, c)
                }
              })
            })
          }
        })
      }, { fiberA, fiberB, c ->
        c.fold({ error ->
          raiseError<D>(error)
        }, { c ->
          racePair(fiberA.join(), fiberB.join()).flatMap {
            it.fold({ attemptedA, fiberB ->
              attemptedA.fold({ error ->
                raiseError<D>(error)
              }, { a ->
                fiberB.join().rethrow().map { b ->
                  f(a, b, c)
                }
              })
            }, { fiberA, attemptedB ->
              attemptedB.fold({ error ->
                raiseError(error)
              }, { b ->
                fiberA.join().rethrow().map { a ->
                  f(a, b, c)
                }
              })
            })
          }
        })
      })
    }
  }, release = { (fiberA, fiberB, fiberC), ex ->
    when (ex) {
      is ExitCase.Completed -> unit()
      else ->
        fiberA.cancel().followedBy(fiberB.cancel()).followedBy(fiberC.cancel())
    }
  })
}