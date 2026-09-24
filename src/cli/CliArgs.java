package cli;

// Structure immuable contenant les options et arguments parsés de la ligne de commande
public record CliArgs(
        boolean isHelp,
        boolean isVersion,
        boolean isVerbose,
        boolean isRaw,
        boolean isDryRun,
        boolean isClipboard,
        boolean isExec,
        String instruction,
        String code,
        String agent,
        String output,
        String template,
        String domain,
        String language,
        String filePath,
        String model,
        boolean isModelsList,
        boolean isModelsPurge,
        String modelsDelete,
        String modelsInfo,
        String modelsInstall,
        boolean isModelsCheck,
        boolean isYes,
        boolean isImproveJson,
        boolean isRuntimeInstall,
        boolean isCorpusInstall
) {
    public boolean hasInstruction() {
        return instruction != null && !instruction.isBlank();
    }

    public boolean hasCode() {
        return code != null && !code.isBlank();
    }

    public boolean hasAgent() {
        return agent != null && !agent.isBlank();
    }

    public boolean hasOutput() {
        return output != null && !output.isBlank();
    }

    public boolean hasTemplate() {
        return template != null && !template.isBlank();
    }

    public boolean hasDomain() {
        return domain != null && !domain.isBlank();
    }

    public boolean hasLanguage() {
        return language != null && !language.isBlank();
    }

    public boolean hasFilePath() {
        return filePath != null && !filePath.isBlank();
    }

    public boolean hasModelsDelete() {
        return modelsDelete != null && !modelsDelete.isBlank();
    }

    public boolean hasModelsInfo() {
        return modelsInfo != null && !modelsInfo.isBlank();
    }

    public boolean hasModelsInstall() {
        return modelsInstall != null && !modelsInstall.isBlank();
    }

    // Vrai des qu'une commande de gestion des modeles est demandee : ces commandes
    // court-circuitent le pipeline de generation.
    public boolean isCommandeModeles() {
        return isModelsList || isModelsPurge || hasModelsDelete() || hasModelsInfo() || hasModelsInstall() || isModelsCheck
                || isRuntimeInstall || isCorpusInstall;
    }

    public boolean hasModel() {
        return model != null && !model.isBlank();
    }

    // Combine l'instruction et le code injecté pour l'analyse NLP
    public String getFullPrompt() {
        StringBuilder sb = new StringBuilder();
        if (hasInstruction()) {
            sb.append(instruction.trim());
        }
        if (hasCode()) {
            if (sb.length() > 0) {
                sb.append("\n\n```\n").append(code.trim()).append("\n```");
            } else {
                sb.append(code.trim());
            }
        }
        return sb.toString().trim();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean isHelp = false;
        private boolean isVersion = false;
        private boolean isVerbose = false;
        private boolean isRaw = false;
        private boolean isDryRun = false;
        private boolean isClipboard = false;
        private boolean isExec = false;
        private String instruction = null;
        private String code = null;
        private String agent = null;
        private String output = null;
        private String template = null;
        private String domain = null;
        private String language = null;
        private String filePath = null;
        private String model = null;
        private boolean isModelsList = false;
        private boolean isModelsPurge = false;
        private String modelsDelete = null;
        private String modelsInfo = null;
        private String modelsInstall = null;
        private boolean isModelsCheck = false;
        private boolean isYes = false;
        private boolean isImproveJson = false;
        private boolean isRuntimeInstall = false;
        private boolean isCorpusInstall = false;

        public Builder help(boolean isHelp) { this.isHelp = isHelp; return this; }
        public Builder version(boolean isVersion) { this.isVersion = isVersion; return this; }
        public Builder verbose(boolean isVerbose) { this.isVerbose = isVerbose; return this; }
        public Builder raw(boolean isRaw) { this.isRaw = isRaw; return this; }
        public Builder dryRun(boolean isDryRun) { this.isDryRun = isDryRun; return this; }
        public Builder clipboard(boolean isClipboard) { this.isClipboard = isClipboard; return this; }
        public Builder exec(boolean isExec) { this.isExec = isExec; return this; }
        public Builder instruction(String instruction) { this.instruction = instruction; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder agent(String agent) { this.agent = agent; return this; }
        public Builder output(String output) { this.output = output; return this; }
        public Builder template(String template) { this.template = template; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder modelsList(boolean isModelsList) { this.isModelsList = isModelsList; return this; }
        public Builder modelsPurge(boolean isModelsPurge) { this.isModelsPurge = isModelsPurge; return this; }
        public Builder modelsDelete(String modelsDelete) { this.modelsDelete = modelsDelete; return this; }
        public Builder modelsInfo(String modelsInfo) { this.modelsInfo = modelsInfo; return this; }
        public Builder modelsInstall(String modelsInstall) { this.modelsInstall = modelsInstall; return this; }
        public Builder modelsCheck(boolean isModelsCheck) { this.isModelsCheck = isModelsCheck; return this; }
        public Builder yes(boolean isYes) { this.isYes = isYes; return this; }
        public Builder improveJson(boolean isImproveJson) { this.isImproveJson = isImproveJson; return this; }
        public Builder runtimeInstall(boolean isRuntimeInstall) { this.isRuntimeInstall = isRuntimeInstall; return this; }
        public Builder corpusInstall(boolean isCorpusInstall) { this.isCorpusInstall = isCorpusInstall; return this; }

        public CliArgs build() {
            return new CliArgs(
                    isHelp, isVersion, isVerbose, isRaw, isDryRun, isClipboard, isExec,
                    instruction, code, agent, output, template, domain, language, filePath, model,
                    isModelsList, isModelsPurge, modelsDelete, modelsInfo, modelsInstall, isModelsCheck, isYes, isImproveJson,
                    isRuntimeInstall, isCorpusInstall
            );
        }
    }
}
