package com.manage.crm.user.domain.repository

import com.manage.crm.user.UserModuleTestTemplate
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.vo.UserAttributes
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class UserCustomRepositoryTest(
    private val userRepository: UserRepository,
) : UserModuleTestTemplate() {
    init {
        given("user repository") {
            afterEach {
                userRepository.deleteAll()
            }

            then("findByEmail returns null when user does not exist") {
                userRepository.findByEmail("not-exists@example.com").shouldBeNull()
            }

            then("findByEmail returns matching user when email exists") {
                val email = "find-by-email-${System.currentTimeMillis()}@example.com"
                val saved =
                    userRepository.save(
                        User.new(
                            externalId = "external-${System.currentTimeMillis()}",
                            userAttributes =
                                UserAttributes(
                                    """
                                    {
                                      "email": "$email",
                                      "name": "Find By Email"
                                    }
                                    """.trimIndent(),
                                ),
                        ),
                    )

                val found = userRepository.findByEmail(email)
                requireNotNull(found)

                found.id shouldBe saved.id
                found.externalId shouldBe saved.externalId
            }
        }
    }
}
