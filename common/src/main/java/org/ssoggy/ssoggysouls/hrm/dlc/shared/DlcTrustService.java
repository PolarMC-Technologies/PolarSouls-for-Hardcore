package org.ssoggy.ssoggysouls.hrm.dlc.shared;

import java.util.Map;
import java.util.UUID;

public final class DlcTrustService {
    private DlcTrustService() {
    }

    public record TrustResult(DlcCommandResult result, String targetMessage) {
    }

    public static TrustResult execute(UUID playerUuid,
                                      String playerName,
                                      UUID targetUuid,
                                      String targetName,
                                      DlcTrustAction action) {
        DlcNames.cache(playerUuid, playerName);
        if (targetUuid != null && targetName != null) {
            DlcNames.cache(targetUuid, targetName);
        }

        if (action == DlcTrustAction.INFO) {
            return new TrustResult(showTrustList(playerUuid), null);
        }

        if (targetUuid == null || targetName == null || targetName.isBlank()) {
            return new TrustResult(DlcCommandResult.fail("Please use /trust <action> [player]"), null);
        }

        if (playerUuid.equals(targetUuid)) {
            return new TrustResult(DlcCommandResult.fail("Player has you blocked"), null);
        }

        DlcSocial social = new DlcSocial(playerUuid);
        DlcSocial targetSocial = new DlcSocial(targetUuid);
        DlcRelation currentRelation = social.getRelationTo(targetUuid);
        DlcRelation theirRelation = targetSocial.getRelationTo(playerUuid);

        return switch (action) {
            case BLOCK -> handleBlock(social, targetSocial, playerUuid, targetUuid, targetName, currentRelation, theirRelation);
            case REVOKE -> handleRevoke(social, targetUuid, targetName, currentRelation);
            case GRANT -> handleGrant(social, targetSocial, playerUuid, targetUuid, targetName, playerName, currentRelation, theirRelation);
            case INFO -> new TrustResult(showTrustList(playerUuid), null);
        };
    }

    private static TrustResult handleBlock(DlcSocial social,
                                           DlcSocial targetSocial,
                                           UUID playerUuid,
                                           UUID targetUuid,
                                           String targetName,
                                           DlcRelation currentRelation,
                                           DlcRelation theirRelation) {
        if (currentRelation == DlcRelation.BLOCKED) {
            return new TrustResult(DlcCommandResult.info("You already blocked " + targetName), null);
        }

        boolean changed = social.setRelationTo(targetUuid, DlcRelation.BLOCKED);
        if (changed) {
            social.saveChanges();
        }
        if (theirRelation.isTrustworthy()) {
            boolean targetChanged = targetSocial.setRelationTo(playerUuid, DlcRelation.UNTRUSTED);
            if (targetChanged) {
                targetSocial.saveChanges();
            }
        }
        return new TrustResult(DlcCommandResult.success("You have blocked " + targetName), null);
    }

    private static TrustResult handleRevoke(DlcSocial social,
                                            UUID targetUuid,
                                            String targetName,
                                            DlcRelation currentRelation) {
        if (currentRelation == DlcRelation.UNTRUSTED) {
            return new TrustResult(DlcCommandResult.info("You have no relations with " + targetName), null);
        }

        boolean changed = social.setRelationTo(targetUuid, null);
        if (changed) {
            social.saveChanges();
        }
        return new TrustResult(DlcCommandResult.success("You no longer trust " + targetName), null);
    }

    @SuppressWarnings("java:S107")
    private static TrustResult handleGrant(DlcSocial social,
                                           DlcSocial targetSocial,
                                           UUID playerUuid,
                                           UUID targetUuid,
                                           String targetName,
                                           String playerName,
                                           DlcRelation currentRelation,
                                           DlcRelation theirRelation) {
        if (currentRelation.isTrustworthy()) {
            return new TrustResult(DlcCommandResult.info("You have already entrusted " + targetName), null);
        }
        if (theirRelation == DlcRelation.BLOCKED) {
            return new TrustResult(DlcCommandResult.fail("Player has you blocked."), null);
        }
        if (theirRelation == DlcRelation.TRUSTED) {
            boolean changed = social.setRelationTo(targetUuid, DlcRelation.FRIENDS);
            boolean targetChanged = targetSocial.setRelationTo(playerUuid, DlcRelation.FRIENDS);
            if (changed || targetChanged) {
                social.saveChanges();
            }
            return new TrustResult(
                    DlcCommandResult.success("You are now friends with " + targetName),
                    "You are now friends with " + playerName
            );
        }

        boolean changed = social.setRelationTo(targetUuid, DlcRelation.TRUSTED);
        if (changed) {
            social.saveChanges();
        }
        return new TrustResult(DlcCommandResult.success("You have now entrusted " + targetName), null);
    }

    private static DlcCommandResult showTrustList(UUID playerUuid) {
        StringBuilder builder = new StringBuilder("--- Trust List ---");
        Map<UUID, DlcRelation> relations = new DlcSocial(playerUuid).getRelationsToAll((uuid, relation) -> relation != DlcRelation.UNTRUSTED);
        if (relations.isEmpty()) {
            return DlcCommandResult.info("Your trust list is empty.");
        }

        relations.forEach((uuid, relation) -> {
            String username = DlcNames.getOrDefault(uuid, uuid.toString());
            username = (username != null) ? username : "Unknown";
            builder.append('\n').append("- ").append(username).append(": ").append(relation);
        });
        return DlcCommandResult.raw(builder.toString());
    }
}
