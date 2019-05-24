package com.anaykamat.examples.kotlin.freemonads.step1


fun <A,B,C> ((A) -> B).andThen(f:(B)->C):(A) -> C = { a -> f(this(a))}

sealed class Instructions<A>{

    data class Write<A>(val message:String, val next:A):Instructions<A>()
    data class Read<A>(val next:(String) -> A):Instructions<A>()

}

fun <A,B> Instructions<A>.map(f:(A) -> B):Instructions<B>{
    return when(this){
        is Instructions.Write<A> -> Instructions.Write<B>(message, f(this.next))
        is Instructions.Read<A> -> Instructions.Read<B>(next.andThen(f))
    }
}

sealed class FreeType<A>{
    data class Free<A>(val data:Instructions<FreeType<A>>):FreeType<A>()
    data class Pure<A>(val data: A):FreeType<A>()
}

fun <A> interpretInstructions(instruction:Instructions<A>):A{
    return when(instruction){
        is Instructions.Write<A> -> {
            println(instruction.message)
            instruction.next
        }
        is Instructions.Read<A> -> {
            val x:String = readLine() ?: ""
            instruction.next(x)
        }
    }
}

fun <A> interpretFree(free:FreeType<A>, interpreter:(Instructions<FreeType<A>>) -> FreeType<A>):A{
    return when(free){
        is FreeType.Free<A> -> interpretFree(interpreter(free.data), interpreter)
        is FreeType.Pure<A> -> free.data
    }
}

fun main(args:Array<String>){

    val freeProgram = FreeType.Free(
        Instructions.Write("What's your name",
                    FreeType.Free(Instructions.Read({x ->
                        FreeType.Pure(Unit) as FreeType<Unit>
                    }) as Instructions<FreeType<Unit>>
            ) as FreeType<Unit>
        )
    )

    interpretFree(freeProgram, ::interpretInstructions)
}
