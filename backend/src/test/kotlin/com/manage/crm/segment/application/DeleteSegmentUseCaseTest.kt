package com.manage.crm.segment.application

import com.manage.crm.segment.application.dto.DeleteSegmentUseCaseIn
import com.manage.crm.segment.domain.Segment
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.NotFoundByIdException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class DeleteSegmentUseCaseTest : BehaviorSpec({
    lateinit var segmentRepository: SegmentRepository
    lateinit var useCase: DeleteSegmentUseCase

    beforeTest {
        segmentRepository = mockk(relaxed = true)
        useCase = DeleteSegmentUseCase(
            segmentRepository = segmentRepository
        )
    }

    given("UC-SEGMENT-004 DeleteSegmentUseCase") {
        `when`("segment exists") {
            then("delete segment once") {
                val segmentId = 10L
                val segment = Segment.new(
                    id = segmentId,
                    name = "active-users",
                    description = "desc",
                    active = true
                )

                coEvery { segmentRepository.findById(segmentId) } returns segment

                useCase.execute(DeleteSegmentUseCaseIn(id = segmentId))

                coVerify(exactly = 1) { segmentRepository.delete(segment) }
            }
        }

        `when`("segment does not exist") {
            then("throw not found exception") {
                val segmentId = 999L
                coEvery { segmentRepository.findById(segmentId) } returns null

                shouldThrow<NotFoundByIdException> {
                    useCase.execute(DeleteSegmentUseCaseIn(id = segmentId))
                }
            }
        }
    }
})
