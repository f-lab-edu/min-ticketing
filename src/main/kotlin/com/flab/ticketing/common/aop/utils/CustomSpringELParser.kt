package com.flab.ticketing.common.aop.utils


import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext


// 컬리의 https://helloworld.kurly.com/blog/distributed-redisson-lock/ 글을 참고하였습니다.
object CustomSpringELParser {
    fun getDynamicValue(parameterNames: Array<String>, args: Array<Any>, expression: String): Any? {
        val parser: ExpressionParser = SpelExpressionParser()
        val context = StandardEvaluationContext()

        parameterNames.zip(args).forEach { (name, value) ->
            context.setVariable(name, value)
        }

        return parser.parseExpression(expression).getValue(context, Any::class.java)
    }
}