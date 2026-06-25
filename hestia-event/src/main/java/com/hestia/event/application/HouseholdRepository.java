package com.hestia.event.application;

import com.hestia.event.domain.ConnectedAccount;
import com.hestia.event.domain.Household;
import com.hestia.event.domain.HouseholdMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for household/member/connected-account reads. */
public interface HouseholdRepository {

    /**
     * The single seeded demo household for this slice. Every authenticated user views it (there is
     * no client-supplied household id, so no IDOR surface — see TAC-1).
     */
    Optional<Household> findDefaultHousehold();

    List<HouseholdMember> members(UUID householdId);

    List<ConnectedAccount> connectedAccounts(UUID householdId);
}
