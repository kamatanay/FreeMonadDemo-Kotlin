package com.anaykamat.examples.kotlin.freemonads.step4

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

fun <A,B> FreeType<A>.bind(f:(A) -> FreeType<B>):FreeType<B>{
    return when(this){
        is FreeType.Pure<A> -> f(this.data)
        is FreeType.Free<A> -> FreeType.Free(this.data.map({free -> free.bind(f)}))
    }
}


fun <A> liftToFree(value:Instructions<A>):FreeType<A>{
    return FreeType.Free<A>(value.map { value -> FreeType.Pure(value) })
}

infix fun <A,B>  FreeType<A>.`))==`(f:(A) -> Instructions<B>):FreeType<B> = this.bind { value -> liftToFree(f(value)) }
infix fun <A,B>  FreeType<A>.`))`(instruction:Instructions<B>):FreeType<B> = this.bind { value -> liftToFree(instruction) }



tailrec fun continueHelloTillNo(response:String){
    if (response == "N") return
    val response = interpretFree(printHelloAndAskForInput(), ::interpretInstructions)
    continueHelloTillNo(response)
}

fun printHelloAndAskForInput():FreeType<String> = {
    liftToFree(Instructions.Write("Hello There!!!!", Unit)) `))`
    Instructions.Write("Do you want to continue? (Y/N)", Unit) `))`
    Instructions.Read({ value -> value })
}()

fun main(args:Array<String>){
    continueHelloTillNo("Y")
}
