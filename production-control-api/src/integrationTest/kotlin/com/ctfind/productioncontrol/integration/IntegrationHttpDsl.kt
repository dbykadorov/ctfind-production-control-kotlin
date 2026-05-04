package com.ctfind.productioncontrol.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

fun IntegrationTestSupport.getJson(path: String, token: String? = null): MvcResult =
	mockMvc.perform(auth(get(path).accept(MediaType.APPLICATION_JSON), token)).andReturn()

fun IntegrationTestSupport.postJson(path: String, body: Any, token: String? = null): MvcResult =
	mockMvc.perform(
		auth(
			post(path)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)),
			token,
		),
	).andReturn()

fun IntegrationTestSupport.putJson(path: String, body: Any, token: String? = null): MvcResult =
	mockMvc.perform(
		auth(
			put(path)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)),
			token,
		),
	).andReturn()

fun IntegrationTestSupport.patchJson(path: String, token: String? = null): MvcResult =
	mockMvc.perform(auth(patch(path).accept(MediaType.APPLICATION_JSON), token)).andReturn()

fun IntegrationTestSupport.deleteJson(path: String, token: String? = null): MvcResult =
	mockMvc.perform(auth(delete(path).accept(MediaType.APPLICATION_JSON), token)).andReturn()

fun MvcResult.assertStatus(expected: Int): MvcResult {
	assertEquals(expected, response.status, response.contentAsString)
	return this
}

fun MvcResult.assertOk(): MvcResult = assertStatus(200)

fun MvcResult.assertCreated(): MvcResult = assertStatus(201)

fun MvcResult.assertNoContent(): MvcResult = assertStatus(204)

fun MvcResult.assertUnauthorized(): MvcResult = assertStatus(401)

fun MvcResult.assertForbidden(): MvcResult = assertStatus(403)

fun MvcResult.assertConflict(): MvcResult = assertStatus(409)

private fun auth(builder: MockHttpServletRequestBuilder, token: String?): MockHttpServletRequestBuilder =
	token?.let { builder.header(HttpHeaders.AUTHORIZATION, "Bearer $it") } ?: builder
