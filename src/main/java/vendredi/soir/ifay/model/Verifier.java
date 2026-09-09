package vendredi.soir.ifay.model;

/**
 * The verifier app that reported a payment - identified by {@code appId} (reverse-domain, e.g.
 * {@code mg.langio.porofo}), its release {@code version}, and an optional {@code revision} (e.g.
 * a commit sha) for tracing an unreleased/dev build more precisely than the version alone.
 */
public record Verifier(String id, String appId, String version, String revision) {}
