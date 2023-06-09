package com.cosain.trilo.unit.trip.presentation.trip.query.docs;

import com.cosain.trilo.support.RestDocsTestSupport;
import com.cosain.trilo.trip.application.trip.query.usecase.TemporarySearchUseCase;
import com.cosain.trilo.trip.infra.dto.ScheduleSummary;
import com.cosain.trilo.trip.presentation.trip.query.TripTemporaryStorageQueryController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

@WebMvcTest(TripTemporaryStorageQueryController.class)
public class TripTemporaryStorageQueryControllerDocsTest extends RestDocsTestSupport {

    @MockBean
    private TemporarySearchUseCase temporarySearchUseCase;
    private final String ACCESS_TOKEN = "Bearer accessToken";

    @Test
    void 임시보관함_조회() throws Exception{
        Long tripId = 1L;
        mockingForLoginUserAnnotation();
        ScheduleSummary scheduleSummary1 = new ScheduleSummary(1L, "제목", "장소 이름","장소 식별자", 33.33, 33.33);
        ScheduleSummary scheduleSummary2 = new ScheduleSummary(2L, "제목", "장소 이름","장소 식별자",33.33, 33.33);
        SliceImpl<ScheduleSummary> scheduleSummaries = new SliceImpl<>(List.of(scheduleSummary1, scheduleSummary2));
        given(temporarySearchUseCase.searchTemporary(eq(tripId), any(Pageable.class))).willReturn(scheduleSummaries);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/trips/{tripId}/temporary-storage", tripId)
                .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                .characterEncoding(StandardCharsets.UTF_8)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                        .description("Bearer 타입 AccessToken")
                        ),
                        pathParameters(
                                parameterWithName("tripId").description("조회할 여행 ID")
                        ),
                        responseFields(
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                                subsectionWithPath("tempSchedules").type(ARRAY).description("임시 일정 목록")
                        )
                )).andDo(restDocs.document(
                        responseFields(beneathPath("tempSchedules").withSubsectionId("tempSchedules"),
                                fieldWithPath("scheduleId").type(NUMBER).description("일정 ID"),
                                fieldWithPath("title").type(STRING).description("제목"),
                                fieldWithPath("placeName").type(STRING).description("장소 이름"),
                                fieldWithPath("placeId").type(STRING).description("장소 식별자"),
                                subsectionWithPath("coordinate").type(OBJECT).description("장소의 좌표")
                        )
                )).andDo(restDocs.document(
                        responseFields(beneathPath("tempSchedules[].coordinate").withSubsectionId("coordinate"),
                                fieldWithPath("latitude").type(NUMBER).description("위도"),
                                fieldWithPath("longitude").type(NUMBER).description("경도")
                        )
                ));
    }

}
