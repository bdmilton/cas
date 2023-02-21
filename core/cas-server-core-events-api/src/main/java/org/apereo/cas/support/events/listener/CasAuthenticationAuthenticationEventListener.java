package org.apereo.cas.support.events.listener;

import org.apereo.cas.authentication.adaptive.geo.GeoLocationRequest;
import org.apereo.cas.authentication.adaptive.geo.GeoLocationService;
import org.apereo.cas.support.events.AbstractCasEvent;
import org.apereo.cas.support.events.CasEventRepository;
import org.apereo.cas.support.events.authentication.CasAuthenticationPolicyFailureEvent;
import org.apereo.cas.support.events.authentication.CasAuthenticationTransactionFailureEvent;
import org.apereo.cas.support.events.authentication.adaptive.CasRiskyAuthenticationDetectedEvent;
import org.apereo.cas.support.events.dao.CasEvent;
import org.apereo.cas.support.events.dao.ClientInfoDTO;
import org.apereo.cas.support.events.ticket.CasTicketGrantingTicketCreatedEvent;
import org.apereo.cas.support.events.ticket.CasTicketGrantingTicketDestroyedEvent;
import org.apereo.cas.util.DateTimeUtils;
import org.apereo.cas.util.HttpRequestUtils;
import org.apereo.cas.util.text.MessageSanitizer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Instant;

/**
 * This is {@link CasAuthenticationAuthenticationEventListener} that attempts to consume CAS events
 * upon various authentication events. Event data is persisted into a repository
 * via {@link CasEventRepository}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@RequiredArgsConstructor
@Getter
@Slf4j
public class CasAuthenticationAuthenticationEventListener implements CasAuthenticationEventListener {

    private final CasEventRepository casEventRepository;

    private final MessageSanitizer messageSanitizer;

    private final GeoLocationService geoLocationService;

    private CasEvent prepareCasEvent(final AbstractCasEvent event,final ClientInfoDTO clientInfoDTO) {
        val dto = new CasEvent();
        dto.setType(event.getClass().getCanonicalName());
        dto.putTimestamp(event.getTimestamp());
        val dt = DateTimeUtils.zonedDateTimeOf(Instant.ofEpochMilli(event.getTimestamp()));
        dto.setCreationTime(dt.toString());
        if (clientInfoDTO != null && clientInfoDTO.isInitialized()) {
            dto.putClientIpAddress(clientInfoDTO.getClientIpAddress());
            dto.putServerIpAddress(clientInfoDTO.getServerIpAddress());
            dto.putAgent(clientInfoDTO.getUserAgent());
            val location = determineGeoLocationFor(clientInfoDTO);
            dto.putGeoLocation(location);
        } else {
            LOGGER.trace("No client information is available. The final event cannot track client location, user agent or IP addresses");
        }
        return dto;
    }

    private GeoLocationRequest determineGeoLocationFor(final ClientInfoDTO clientInfo) {
        val geoLocationRequest = HttpRequestUtils.getHttpServletRequestGeoLocation(clientInfo.getGeoLocation());
        if (!geoLocationRequest.isValid() && geoLocationService != null) {
            val geoResponse = geoLocationService.locate(clientInfo.getClientIpAddress());
            if (geoResponse != null) {
                return new GeoLocationRequest(geoResponse.getLatitude(), geoResponse.getLongitude());
            }
        }
        return geoLocationRequest;
    }

    @Override
    public void handleCasTicketGrantingTicketCreatedEvent(final CasTicketGrantingTicketCreatedEvent event) throws Exception {
        val dto = prepareCasEvent(event,event.getClientInfoDTO());
        dto.setCreationTime(event.getTicketGrantingTicket().getCreationTime().toString());
        dto.putEventId(messageSanitizer.sanitize(event.getTicketGrantingTicket().getId()));
        dto.setPrincipalId(event.getTicketGrantingTicket().getAuthentication().getPrincipal().getId());
        this.casEventRepository.save(dto);
    }

    @Override
    public void handleCasTicketGrantingTicketDeletedEvent(final CasTicketGrantingTicketDestroyedEvent event) throws Exception {
        val dto = prepareCasEvent(event,event.getClientInfoDTO());
        dto.setCreationTime(event.getTicketGrantingTicket().getCreationTime().toString());
        dto.putEventId(messageSanitizer.sanitize(event.getTicketGrantingTicket().getId()));
        dto.setPrincipalId(event.getTicketGrantingTicket().getAuthentication().getPrincipal().getId());
        this.casEventRepository.save(dto);
    }

    @Override
    public void handleCasAuthenticationTransactionFailureEvent(final CasAuthenticationTransactionFailureEvent event) throws Exception {
        val dto = prepareCasEvent(event,event.getClientInfoDTO());
        dto.setPrincipalId(event.getCredential().getId());
        dto.putEventId(CasAuthenticationTransactionFailureEvent.class.getSimpleName());
        this.casEventRepository.save(dto);
    }

    @Override
    public void handleCasAuthenticationPolicyFailureEvent(final CasAuthenticationPolicyFailureEvent event) throws Exception {
        val dto = prepareCasEvent(event,event.getClientInfoDTO());
        dto.setPrincipalId(event.getAuthentication().getPrincipal().getId());
        dto.putEventId(CasAuthenticationPolicyFailureEvent.class.getSimpleName());
        this.casEventRepository.save(dto);
    }

    @Override
    public void handleCasRiskyAuthenticationDetectedEvent(final CasRiskyAuthenticationDetectedEvent event) throws Exception {
        val dto = prepareCasEvent(event,event.getClientInfoDTO());
        dto.putEventId(event.getService().getName());
        dto.setPrincipalId(event.getAuthentication().getPrincipal().getId());
        this.casEventRepository.save(dto);
    }
}
