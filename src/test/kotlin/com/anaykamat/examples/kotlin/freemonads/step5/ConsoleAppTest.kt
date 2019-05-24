package com.anaykamat.examples.kotlin.freemonads.step5

import org.junit.Assert
import org.junit.Test

class ConsoleAppTest {


    fun <A> interpretPure(inputs:MutableList<String>, outputs: MutableList<String>, instruction:Instructions<A>):A{
        return when(instruction){
            is Instructions.Write<A> -> {
                outputs.add(instruction.message)
                instruction.next
            }
            is Instructions.Read<A> -> {
                val x:String = inputs.getOrNull(0)?.also { inputs.removeAt(0) } ?: ""
                instruction.next(x)
            }
        }
    }

    val pureInterpreter:(MutableList<String>, MutableList<String>, Instructions<FreeType<String>>) -> FreeType<String> = ::interpretPure

    @Test
    fun itShouldDoNoThingIfStartedWithInputAsNo(){
        val inputs = mutableListOf<String>()
        val outputs = mutableListOf<String>()

        continueHelloTillNo("N", pureInterpreter.toCurried()(inputs)(outputs))

        Assert.assertEquals(emptyList<String>(), inputs.toList())
        Assert.assertEquals(emptyList<String>(), outputs.toList())
    }

    @Test
    fun itShouldDisplayMessageButNotContinueIfResponseFromConsoleIsNo(){
        val inputs = mutableListOf("N")
        val outputs = mutableListOf<String>()

        continueHelloTillNo("Y", pureInterpreter.toCurried()(inputs)(outputs))

        Assert.assertEquals(emptyList<String>(), inputs.toList())
        Assert.assertEquals(listOf("Hello There!!!!","Do you want to continue? (Y/N)"), outputs.toList())
    }

    @Test
    fun itShouldDisplayMessageAndContinueWhileResponseFromConsoleIsYes(){
        val inputs = mutableListOf("Y","Y","N")
        val outputs = mutableListOf<String>()

        continueHelloTillNo("Y", pureInterpreter.toCurried()(inputs)(outputs))

        Assert.assertEquals(emptyList<String>(), inputs.toList())
        Assert.assertEquals(listOf(
            "Hello There!!!!",
            "Do you want to continue? (Y/N)",
            "Hello There!!!!",
            "Do you want to continue? (Y/N)",
            "Hello There!!!!",
            "Do you want to continue? (Y/N)"
        ), outputs.toList())
    }

}