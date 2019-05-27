package com.anaykamat.examples.kotlin.freemonads.step6

fun <A,B,C> ((A) -> B).andThen(f:(B)->C):(A) -> C = { a -> f(this(a))}

fun <A,B,C,D> ((A,B,C) -> D).toCurried():(A) -> (B) -> (C) -> D = { a -> {b -> {c -> this(a,b,c) } }}

interface Kind<F,A>

class ForInstructions private constructor() {}

inline fun <A> Kind<ForInstructions, A>.fix():Instructions<A> = this as Instructions<A>

sealed class Instructions<A>:Kind<ForInstructions, A>{
    companion object {

    }
    data class Write<A>(val message:String, val next:A):Instructions<A>()
    data class Read<A>(val next:(String) -> A):Instructions<A>()

}

interface Functor<F>{
    fun <A,B> Kind<F,A>.map(f:(A) -> B):Kind<F,B>
}

object FunctorForInstructions:Functor<ForInstructions>{
    override fun <A, B> Kind<ForInstructions, A>.map(f: (A) -> B): Kind<ForInstructions, B>{
        val value = this.fix()
        return when(value){
            is Instructions.Write<A> -> Instructions.Write(value.message, f(value.next))
            is Instructions.Read<A> -> Instructions.Read(value.next.andThen(f))
        }
    }

}

fun Instructions.Companion.functor():FunctorForInstructions = FunctorForInstructions


sealed class FreeType<F,A>(open val functor: Functor<F>){
    data class Free<F,A>(val data:Kind<F,FreeType<F,A>>, override val functor: Functor<F>):FreeType<F,A>(functor)
    data class Pure<F,A>(val data: A, override val functor: Functor<F>):FreeType<F,A>(functor)
}

fun <A> interpretInstructions(instruction:Kind<ForInstructions,A>):A{
    val value = instruction.fix()
    return when(value){
        is Instructions.Write<A> -> {
            println(value.message)
            value.next
        }
        is Instructions.Read<A> -> {
            val x:String = readLine() ?: ""
            value.next(x)
        }
    }
}

fun <F,A> interpretFree(free:FreeType<F,A>, interpreter:(Kind<F,FreeType<F,A>>) -> FreeType<F,A>):A{
    return when(free){
        is FreeType.Free<F,A> -> interpretFree(interpreter(free.data), interpreter)
        is FreeType.Pure<F,A> -> free.data
    }
}

fun <F,A,B> FreeType<F,A>.bind(f:(A) -> FreeType<F,B>):FreeType<F,B>{
    val value = this
    return when(value){
        is FreeType.Pure<F,A> -> f(value.data)
        is FreeType.Free<F,A> -> value.functor.run {
            FreeType.Free(value.data.map { freeType -> freeType.bind(f) }, value.functor)
        }
    }
}


fun <F,A> liftToFree(functor:Functor<F>, value:Kind<F,A>):FreeType<F,A>{
    return functor.run {
        FreeType.Free<F,A>(value.map { value -> FreeType.Pure<F,A>(value, functor) }, functor)
    }
}

infix fun <F,A,B>  FreeType<F,A>.`))==`(f:(A) -> Kind<F,B>):FreeType<F,B> = this.bind{ value -> liftToFree(functor,f(value)) }
infix fun <F,A,B>  FreeType<F,A>.`))`(instruction:Kind<F,B>):FreeType<F,B> = this.bind { value -> liftToFree(functor, instruction) }


tailrec fun continueHelloTillNo(response:String, interpreter:(Kind<ForInstructions,FreeType<ForInstructions,String>>) -> FreeType<ForInstructions,String>){
    if (response == "N") return
    val response = interpretFree(printHelloAndAskForInput(), interpreter)
    continueHelloTillNo(response, interpreter)
}

fun printHelloAndAskForInput():FreeType<ForInstructions, String> = {
    liftToFree(Instructions.functor(),Instructions.Write("Hello There!!!!", Unit)) `))`
            Instructions.Write("Do you want to continue? (Y/N)", Unit) `))`
            Instructions.Read({ value -> value })
}()

fun main(args:Array<String>){
    continueHelloTillNo("Y", ::interpretInstructions)
}
