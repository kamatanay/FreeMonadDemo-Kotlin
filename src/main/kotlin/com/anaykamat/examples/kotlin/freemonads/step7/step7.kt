package com.anaykamat.examples.kotlin.freemonads.step7

fun <A,B,C> ((A) -> B).andThen(f:(B)->C):(A) -> C = { a -> f(this(a))}

fun <A,B,C,D> ((A,B,C) -> D).toCurried():(A) -> (B) -> (C) -> D = { a -> {b -> {c -> this(a,b,c) } }}

interface Kind<out F,out A>

interface Functor<F>{
    fun <A,B> Kind<F,A>.map(f:(A) -> B):Kind<F,B>
    fun <A> Kind<F,Any>.flatten():Kind<F,A>
}

class ForCommands private constructor(){}

sealed class Commands<A>:Kind<ForCommands, A>{
    data class Write(val message:String):Commands<Unit>()
    object Read:Commands<String>()
}

inline fun <A> Kind<ForCommands, A>.fix():Commands<A> = this as Commands<A>

class ForId private constructor(){}

data class Id<A>(val value:A):Kind<ForId, A>{
    companion object

    fun <B> map(f:(A) -> B): Id<B> = Id(f(value))
}
fun <A> Kind<ForId, A>.fix():Id<A> = this as Id<A>

fun Id.Companion.functor():Functor<ForId> = object : Functor<ForId>{
    override tailrec fun <A> Kind<ForId, Any>.flatten(): Kind<ForId, A> {
        val theValue = this.fix().value
        return when (theValue){
            is Id<*> -> theValue.let { it as Kind<ForId, Any> }.flatten()
            else -> this as Kind<ForId, A>
        }
    }

    override fun <A, B> Kind<ForId, A>.map(f: (A) -> B): Kind<ForId, B> = this.fix().map(f)
}

sealed class Free<F,A>{

    data class Pure<F,A>(val value:A):Free<F,A>()
    data class Suspend<F,A>(val value:Kind<F,A>):Free<F,A>()
    data class FlatMapped<F,B,A>(val value:Free<F,B>, val fs:(B) -> Free<F,A>): Free<F,A>()

    companion object {
        fun <F,A> lift(value:Kind<F,A>):Free<F,A>{
            return Suspend(value)
        }
    }

}

fun <F,A,B> Free<F,A>.bind(f:(A) -> Free<F,B>):Free<F,B>{
    return Free.FlatMapped(this, f)
}

fun <F,A,G> Free<F,A>.foldMap(transform:FunctionK<F,G>, functor:Functor<G>, just:Just<G>):Kind<G,A>{
    return when (this){
        is Free.Pure<F,A> -> just.transform(this.value)
        is Free.Suspend<F,A> -> transform.transform(this.value)
        is Free.FlatMapped<F,*,A> -> {
            val flatMappedValue = this.value
            val flatMapFunction = this.fs
            when(flatMappedValue){
                is Free.Pure<F,*> -> {
                    val currentValue = flatMappedValue.value as A
                    val fMapFunction = flatMapFunction as (A) -> Free<F,A>
                    fMapFunction(currentValue).foldMap(transform,functor, just)
                }
                is Free.Suspend<F,*> -> {
                    val currentValue = flatMappedValue.value as Kind<F,A>
                    val fMapFunction = flatMapFunction as (A) -> Free<F,A>
                    functor.run { transform.transform(currentValue).map(fMapFunction).map { it.foldMap(transform, functor, just) }.flatten<A>() }
                }
                is Free.FlatMapped<F,*,*> -> {
                    val currentValue = flatMappedValue as Free<F,A>
                    val fMapFunction = flatMapFunction as (A) -> Free<F,A>
                    functor.run { currentValue.foldMap(transform, functor, just).map(fMapFunction).map { it.foldMap(transform, functor, just) }.flatten<A>() }
                }
            }
        }
    }
}

interface FunctionK<F,G>{
    fun <A> transform(value:Kind<F,A>):Kind<G,A>
}

interface Just<F>{
    fun <A> transform(value:A):Kind<F,A>
}

object CommandToId:FunctionK<ForCommands, ForId>{
    override fun <A> transform(value: Kind<ForCommands, A>): Kind<ForId, A> {
        val command = value.fix()
        return when(command){
            is Commands.Write -> {
                println(command.message)
                Id(Unit)
            }
            is Commands.Read -> {
                Id(readLine() ?: "")
            }
        } as Kind<ForId, A>
    }
}

fun write(text:String):Free<ForCommands, Unit> = Free.lift(Commands.Write(text))
fun read():Free<ForCommands, String> = Free.lift(Commands.Read)

infix fun <F,A,B> Free<F,A>.`))==`(f:(A) -> Free<F,B>):Free<F,B> = this.bind(f)



tailrec fun continueHelloTillNo(
    response: Id<String>,
    functionK: CommandToId,
    functor: Functor<ForId>,
    just: Just<ForId>
){
    if (response == Id("N")) return
    val response = printHelloAndAskForInput().foldMap(functionK, functor, just)
    continueHelloTillNo(response as Id<String>, functionK, functor, just)
}

fun printHelloAndAskForInput():Free<ForCommands, String> = {
    write("Hello There!!!!") `))==`
    { write("Do you want to continue? (Y/N)") } `))==`
    { read() }
}()

fun main(args:Array<String>){

    continueHelloTillNo(Id("Y"), CommandToId, Id.functor(), object:Just<ForId>{
        override fun <A> transform(value: A): Kind<ForId, A> {
            return Id(value)
        }

    })
}