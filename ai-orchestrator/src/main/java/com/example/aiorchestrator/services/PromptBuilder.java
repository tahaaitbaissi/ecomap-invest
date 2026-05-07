package com.example.aiorchestrator.services;

import com.example.aiorchestrator.dto.HexExplanationContextDto;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptBuilder {

    public String system() {
        return """
                Répondez exclusivement en français.

                Vous êtes l’assistant EcoMap Invest. Vous devez rester STRICTEMENT ancré dans les faits fournis.
                INTERDIT d’inventer des POI, des nombres, des sources, ou des mécanismes non présents dans le bundle.
                
                Contrainte de rendu UI:
                - Écrivez avec des espaces et des retours à la ligne.
                - Utilisez des phrases naturelles, pas de mots collés.
                
                Exemple de style attendu (format, espaces, retours à la ligne):
                "Bonjour !
                
                Cette cellule est faible pour le profil X car les concurrents dominent les conducteurs.
                
                Tags clés :
                - tagA : 3 × 1,20 → 3,60
                - tagB : 1 × 0,70 → 0,70"

                Votre objectif:
                - Expliquer clairement en quoi la cellule est forte/faible POUR CE PROFIL (conducteurs vs concurrents).
                - Distinguer:
                  - score heatmap (0–100 après normalisation globale via stretch bounds)
                  - opportunité (si un objet opportunité est présent)

                Format:
                - Réponse courte et actionnable (2–6 paragraphes, éventuellement 3–6 bullets).
                - Citer 2–5 tags clés avec: count × weight → contribution.
                - Si aggregatedFromGridLeaves=true: rappeler que les totaux sont sommés sur plusieurs sous-hexagones.
                - Ne recopie PAS le JSON en sortie. Transforme les faits en texte.
                - Si une information n’existe pas dans les faits, dis explicitement que ce n’est pas disponible.
                """;
    }

    public String userFactsPrompt(HexExplanationContextDto hex, Object opportunity) {
        String drivers =
                hex.drivers().stream()
                        .filter(r -> r.countInsideAcrossLeaves() > 0)
                        .sorted(Comparator.comparingDouble(HexExplanationContextDto.TagContributionRow::weightedContributionAcrossLeaves).reversed())
                        .limit(10)
                        .map(
                                r ->
                                        "{\"tag\":\""
                                                + esc(r.tag())
                                                + "\",\"weight\":"
                                                + String.format(Locale.US, "%.4f", r.weight())
                                                + ",\"count\":"
                                                + r.countInsideAcrossLeaves()
                                                + ",\"contrib\":"
                                                + String.format(Locale.US, "%.4f", r.weightedContributionAcrossLeaves())
                                                + "}")
                        .collect(Collectors.joining(","));

        String competitors =
                hex.competitors().stream()
                        .filter(r -> r.countInsideAcrossLeaves() > 0)
                        .sorted(Comparator.comparingDouble(HexExplanationContextDto.TagContributionRow::weightedContributionAcrossLeaves).reversed())
                        .limit(10)
                        .map(
                                r ->
                                        "{\"tag\":\""
                                                + esc(r.tag())
                                                + "\",\"weight\":"
                                                + String.format(Locale.US, "%.4f", r.weight())
                                                + ",\"count\":"
                                                + r.countInsideAcrossLeaves()
                                                + ",\"contrib\":"
                                                + String.format(Locale.US, "%.4f", r.weightedContributionAcrossLeaves())
                                                + "}")
                        .collect(Collectors.joining(","));

        String oppJson = "null";
        if (opportunity != null) {
            oppJson = "\"<provided>\"";
        }

        return """
                Faits JSON (ne pas recopier tel quel):
                {
                  "profileName": "%s",
                  "profileUserQuery": "%s",
                  "h3Index": "%s",
                  "aggregatedFromGridLeaves": %s,
                  "gridLeafResolution": %d,
                  "gridLeafCount": %d,
                  "averageRawAcrossLeaves": %.6f,
                  "normalizationStretchLow": %.6f,
                  "normalizationStretchHigh": %.6f,
                  "normalizationFlat": %s,
                  "computedDisplayScore": %s,
                  "totalCompetitorPoisUnweightedAcrossLeaves": %d,
                  "populationDensityAvg": %s,
                  "avgIncomeAvg": %s,
                  "drivers": [%s],
                  "competitors": [%s],
                  "opportunity": %s
                }
                """
                .formatted(
                        esc(hex.profileName()),
                        esc(hex.profileUserQuery()),
                        esc(hex.h3Index()),
                        hex.aggregatedFromGridLeaves(),
                        hex.gridLeafResolution(),
                        hex.gridLeafCount(),
                        hex.averageRawAcrossLeaves(),
                        hex.normalizationStretchLow(),
                        hex.normalizationStretchHigh(),
                        hex.normalizationFlat(),
                        hex.computedDisplayScore() == null ? "null" : String.format(Locale.US, "%.2f", hex.computedDisplayScore()),
                        hex.totalCompetitorPoisUnweightedAcrossLeaves(),
                        hex.populationDensityAvg() == null ? "null" : String.format(Locale.US, "%.2f", hex.populationDensityAvg()),
                        hex.avgIncomeAvg() == null ? "null" : String.format(Locale.US, "%.2f", hex.avgIncomeAvg()),
                        drivers,
                        competitors,
                        oppJson);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}

