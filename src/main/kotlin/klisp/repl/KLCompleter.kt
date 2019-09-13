package klisp.repl

import klisp.specialForm
import klisp.stdEnv
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

class KLCompleter : Completer {
    companion object {
        private fun spec() = specialForm.values().map { c ->
            val state = c.state?.invoke()
            val stateStr = if (state !== null) ", current state: $state"
            else ""

            val candidate = Candidate(
                c.name.toLowerCase(),
                c.name.toLowerCase(),
                "repl commands",
                c.docs + stateStr,
                null,
                null,
                true
            )

            val b_candidate = Candidate(
                "(${c.name.toLowerCase()}",
                c.name.toLowerCase(),
                "repl commands",
                c.docs + stateStr,
                null,
                null,
                true
            )

            val ass = c.aliases?.map { a ->
                listOf(
                    Candidate(
                        a,
                        a,
                        "repl commands",
                        c.docs + stateStr,
                        null,
                        null,
                        true
                    ),
                    Candidate(
                        "($a",
                        a,
                        "repl commands",
                        c.docs + stateStr,
                        null,
                        null,
                        true
                    )
                )
            }?.flatten() ?: emptyList()

            ass + candidate + b_candidate
        }.flatten()


        private fun std() = stdEnv.map { (v, t) ->
            listOf(
                Candidate(
                    v.value,
                    v.value,
                    t.docs,
                    null,
                    null,
                    null,
                    true
                ),
                Candidate(
                    "(" + v.value,
                    v.value,
                    t.docs,
                    null,
                    null,
                    null,
                    true
                )
            )
        }.flatten()


        private fun all() = spec() + std()
    }


    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        candidates.addAll(all())
    }
}