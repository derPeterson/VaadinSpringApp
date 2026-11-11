package de.derpeterson.app.i18n;

import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.config.TokenDurationPolicy;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.UnaryOperator;

/** Typed translation getters shared by the application and external browser tests. */
public class MessageTexts {

  private final UnaryOperator<String> translationLookup;

  public MessageTexts(UnaryOperator<String> translationLookup) {
    this.translationLookup =
        Objects.requireNonNull(translationLookup, "Translation lookup required.");
  }

  public String getTranslation(String translationKey) {
    return translationLookup.apply(translationKey);
  }

  public String getFormattedTranslation(String translationKey, Object... formatArguments) {
    return MessageFormat.format(getTranslation(translationKey), formatArguments);
  }

  public String getDatePickerToday() {
    return getTranslation("base.datePicker.today");
  }

  public String getDatePickerCancel() {
    return getTranslation("base.datePicker.cancel");
  }

  public String getDatePickerCalendar() {
    return getTranslation("base.datePicker.calendar");
  }

  public String getUnsavedChangesTitle() {
    return getTranslation("base.unsavedChanges.title");
  }

  public String getUnsavedChangesDescription() {
    return getTranslation("base.unsavedChanges.description");
  }

  public String getUnsavedChangesDiscard() {
    return getTranslation("base.unsavedChanges.discard");
  }

  public String getUnsavedChangesKeepEditing() {
    return getTranslation("base.unsavedChanges.keepEditing");
  }

  public String getAccountLifecycleTab() {
    return getTranslation("account.lifecycle.tab");
  }

  public String getAccountLifecycleIntro() {
    return getTranslation("account.lifecycle.intro");
  }

  public String getAccountDeactivateAction() {
    return getTranslation("account.lifecycle.deactivate");
  }

  public String getAccountDeleteAction() {
    return getTranslation("account.lifecycle.delete");
  }

  public String getAccountDeactivateExplanation() {
    return getTranslation("account.lifecycle.deactivateExplanation");
  }

  public String getAccountDeleteExplanation() {
    return getTranslation("account.lifecycle.deleteExplanation");
  }

  public String getAccountLifecyclePassword() {
    return getTranslation("account.lifecycle.password");
  }

  public String getAccountLifecycleProofUnavailable() {
    return getTranslation("account.lifecycle.proofUnavailable");
  }

  public String getAccountLifecycleInvalidProof() {
    return getTranslation("account.lifecycle.invalidProof");
  }

  public String getAccountLifecycleLastSuperadmin() {
    return getTranslation("account.lifecycle.lastSuperadmin");
  }

  public String getAccountLifecycleFailure() {
    return getTranslation("account.lifecycle.failure");
  }

  public String getAccountReactivateAction() {
    return getTranslation("account.lifecycle.reactivate");
  }

  public String getAccountReactivateIntro() {
    return getTranslation("account.lifecycle.reactivateIntro");
  }

  public String getAccountReactivateConfirm() {
    return getTranslation("account.lifecycle.reactivateConfirm");
  }

  public String getAccountReactivateRequest() {
    return getTranslation("account.lifecycle.requestLink");
  }

  public String getAccountReactivateRequested() {
    return getTranslation("account.lifecycle.requested");
  }

  public String getAccountReactivateSuccess() {
    return getTranslation("account.lifecycle.reactivated");
  }

  public String getAccountReactivateInvalid() {
    return getTranslation("account.lifecycle.invalidLink");
  }

  public String getAccountDeactivatedMailSubject() {
    return getTranslation("account.lifecycle.deactivatedSubject");
  }

  public String getAccountDeactivatedMailBody(String deletionDateText) {
    return getFormattedTranslation("account.lifecycle.deactivatedBody", deletionDateText);
  }

  public String getAccountDeletionReminderSubject() {
    return getTranslation("account.lifecycle.reminderSubject");
  }

  public String getAccountDeletionReminderBody(String deletionDateText) {
    return getFormattedTranslation("account.lifecycle.reminderBody", deletionDateText);
  }

  public String getAccountDeletedMailSubject() {
    return getTranslation("account.lifecycle.deletedSubject");
  }

  public String getAccountDeletedMailBody() {
    return getTranslation("account.lifecycle.deletedBody");
  }

  public String getAccountDeactivationRetentionLabel() {
    return getTranslation("account.lifecycle.retentionLabel");
  }

  public String getAccountDeactivationRetentionHelp() {
    return getTranslation("account.lifecycle.retentionHelp");
  }

  public String getAccountDeactivationReminderLabel() {
    return getTranslation("account.lifecycle.reminderLabel");
  }

  public String getAccountDeactivationReminderHelp() {
    return getTranslation("account.lifecycle.reminderHelp");
  }

  public String getAccountDeactivationEffective() {
    return getTranslation("account.lifecycle.effective");
  }

  // Support-Tickets und Posteingang
  public String getSupportTitle() {
    return getTranslation("support.title");
  }

  public String getSupportTicketNumber(String ticketNumber) {
    return getFormattedTranslation("support.ticketNumber", ticketNumber);
  }

  public String getSupportInboxDescription() {
    return getTranslation("support.inboxDescription");
  }

  public String getSupportPageDescription() {
    return getTranslation("support.pageDescription");
  }

  public String getSupportYou() {
    return getTranslation("support.you");
  }

  public String getSupportAllTickets() {
    return getTranslation("support.allTickets");
  }

  public String getSupportRecoveryCategory() {
    return getTranslation("support.recoveryCategory");
  }

  public String getSupportSelectTicket() {
    return getTranslation("support.selectTicket");
  }

  public String getSupportNoTicketSelected() {
    return getTranslation("support.noTicketSelected");
  }

  public String getSupportRequester() {
    return getTranslation("support.requester");
  }

  public String getSupportSubjectColumn() {
    return getTranslation("support.subjectColumn");
  }

  public String getSupportStatusColumn() {
    return getTranslation("support.statusColumn");
  }

  public String getSupportAssigneeColumn() {
    return getTranslation("support.assigneeColumn");
  }

  // Support-Benachrichtigungen
  public String getSupportRecoverySubject() {
    return getTranslation("support.recoverySubject");
  }

  public String getSupportRecoveryNotification() {
    return getTranslation("support.recoveryNotification");
  }

  public String getSupportReplyNotificationTitle(String ticketNumber) {
    return ticketNumber == null
        ? getTranslation("support.replyNotificationTitle")
        : getFormattedTranslation("support.replyNotificationTicketTitle", ticketNumber);
  }

  public String getSupportReplyNotificationBody() {
    return getTranslation("support.replyNotificationBody");
  }

  public String getSupportRequesterClosedNotificationTitle(String closedTicketNumber) {
    return closedTicketNumber == null
        ? getSupportClosedByRequester()
        : getFormattedTranslation(
            "support.requesterClosedNotificationTicketTitle", closedTicketNumber);
  }

  public String getSupportRequesterClosedNotificationBody() {
    return getTranslation("support.requesterClosedNotificationBody");
  }

  public String getNotificationsTicketAssigned() {
    return getTranslation("notifications.ticketAssigned");
  }

  // Ticketansicht und Sicherheitshinweise
  public String getSupportSubscription() {
    return getTranslation("support.subscription");
  }

  public String getSupportSubscriptionHint() {
    return getTranslation("support.subscriptionHint");
  }

  public String getSupportFocusTicket() {
    return getTranslation("support.focusTicket");
  }

  public String getSupportRestoreSplitView() {
    return getTranslation("support.restoreSplitView");
  }

  public String getSupportSecurityPolicy() {
    return getTranslation("support.securityPolicy");
  }

  public String getSupportUnassignedHint() {
    return getTranslation("support.unassignedHint");
  }

  public String getSupportClosedHint() {
    return getTranslation("support.closedHint");
  }

  public String getSupportRequesterClosedHint() {
    return getTranslation("support.requesterClosedHint");
  }

  public String getSupportUnreadMessages(long unreadMessageCount) {
    return getFormattedTranslation(
        unreadMessageCount == 1 ? "support.unreadMessage" : "support.unreadMessages",
        unreadMessageCount);
  }

  public String getSupportNewActivity() {
    return getTranslation("support.newActivity");
  }

  public String getSupportNoUnlock() {
    return getTranslation("support.noUnlock");
  }

  public String getSupportSensitiveWarning() {
    return getTranslation("support.sensitiveWarning");
  }

  public String getSupportSecurityNoticeLabel() {
    return getTranslation("support.securityNoticeLabel");
  }

  public String getSupportSecurityNotice() {
    return getTranslation("support.securityNotice");
  }

  public String getSupportDescription() {
    return getTranslation("support.description");
  }

  // Antworten und interne Ticketnotizen
  public String getSupportCreate() {
    return getTranslation("support.create");
  }

  public String getSupportReply() {
    return getTranslation("support.reply");
  }

  public String getSupportSendReply() {
    return getTranslation("support.sendReply");
  }

  public String getSupportPublicReply() {
    return getTranslation("support.publicReply");
  }

  public String getSupportEditorMode() {
    return getTranslation("support.editorMode");
  }

  public String getSupportInternalSecurityNotice() {
    return getTranslation("support.internalSecurityNotice");
  }

  public String getSupportTicketCreated() {
    return getTranslation("support.ticketCreated");
  }

  public String getSupportStatusChanged() {
    return getTranslation("support.statusChanged");
  }

  public String getSupportInternal() {
    return getTranslation("support.internal");
  }

  public String getSupportSaveInternal() {
    return getTranslation("support.saveInternal");
  }

  public String getSupportReplyPlaceholder() {
    return getTranslation("support.replyPlaceholder");
  }

  public String getSupportInternalPlaceholder() {
    return getTranslation("support.internalPlaceholder");
  }

  public String getSupportInternalSaved() {
    return getTranslation("support.internalSaved");
  }

  // Ticketaktionen, Status und Rückmeldungen
  public String getSupportClaim() {
    return getTranslation("support.claim");
  }

  public String getSupportClose() {
    return getTranslation("support.close");
  }

  public String getSupportInstructions() {
    return getTranslation("support.instructions");
  }

  public String getSupportQueued() {
    return getTranslation("support.queued");
  }

  public String getSupportSaved() {
    return getTranslation("support.saved");
  }

  public String getSupportFailed() {
    return getTranslation("support.failed");
  }

  public String getSupportOpen() {
    return getTranslation("support.open");
  }

  public String getSupportInProgress() {
    return getTranslation("support.inProgress");
  }

  public String getSupportClosed() {
    return getTranslation("support.closed");
  }

  public String getSupportClosedByRequester() {
    return getTranslation("support.closedByRequester");
  }

  public String getSupportCloseOwn() {
    return getTranslation("support.closeOwn");
  }

  public String getSupportCreated() {
    return getTranslation("support.created");
  }

  public String getSupportReplySaved() {
    return getTranslation("support.replySaved");
  }

  public String getSupportRefreshed() {
    return getTranslation("support.refreshed");
  }

  public String getSupportRefreshFailed() {
    return getTranslation("support.refreshFailed");
  }

  public String getSupportConflict() {
    return getTranslation("support.conflict");
  }

  public String getSupportShortcutDescription() {
    return getTranslation("support.shortcutDescription");
  }

  public String getSupportUnassigned() {
    return getTranslation("support.unassigned");
  }

  public String getSupportEmpty() {
    return getTranslation("support.empty");
  }

  public String getSupportConversation() {
    return getTranslation("support.conversation");
  }

  public String getSupportExistingRequests() {
    return getTranslation("support.existingRequests");
  }

  public String getSupportCases() {
    return getTranslation("support.cases");
  }

  // Ticketfilter und Direktsuche
  public String getSupportFilterDescription() {
    return getTranslation("support.filter.description");
  }

  public String getSupportFilterStatus() {
    return getTranslation("support.filter.status");
  }

  public String getSupportFilterMessages() {
    return getTranslation("support.filter.messages");
  }

  public String getSupportJumpTicketNumber() {
    return getTranslation("support.jump.ticketNumber");
  }

  public String getSupportJumpTicketNumberPlaceholder() {
    return getTranslation("support.jump.ticketNumber.placeholder");
  }

  public String getSupportJumpTicketNumberHint() {
    return getTranslation("support.jump.ticketNumber.hint");
  }

  public String getSupportJumpOpen() {
    return getTranslation("support.jump.open");
  }

  public String getSupportFilterAssignment() {
    return getTranslation("support.filter.assignment");
  }

  public String getSupportFilterMine() {
    return getTranslation("support.filter.mine");
  }

  public String getSupportFilterEmpty() {
    return getTranslation("support.filter.empty");
  }

  public String getSupportQueueEmptyTitle() {
    return getTranslation("support.queue.empty.title");
  }

  public String getSupportQueueEmptyHint() {
    return getTranslation("support.queue.empty.hint");
  }

  public String getSupportMessageCount(int messageCount) {
    return messageCount == 1
        ? getTranslation("support.oneMessage")
        : getFormattedTranslation("support.messageCount", messageCount);
  }

  public String getSupportRequestCount(int requestCount) {
    return requestCount == 1
        ? getTranslation("support.oneRequest")
        : getFormattedTranslation("support.requestCount", requestCount);
  }

  public String getSupportMessageSupport() {
    return getTranslation("support.messageSupport");
  }

  public String getSupportRecoveryInstructions() {
    return getTranslation("support.recoveryInstructions");
  }

  public String getSupportInstructionMessageTitle() {
    return getTranslation("support.instructionMessageTitle");
  }

  // Zwei-Faktor-Anmeldung und Wiederherstellung
  public String getTwoFactorPageTitle() {
    return getTranslation("mfa.page.title");
  }

  public String getTwoFactorAreaLabel(boolean isAccountArea) {
    return getTranslation(isAccountArea ? "mfa.area.account" : "mfa.area.login");
  }

  public String getTwoFactorPageHint() {
    return getTranslation("mfa.page.hint");
  }

  public String getTwoFactorConfirmTitle() {
    return getTranslation("mfa.confirm.title");
  }

  public String getTwoFactorConfirmSubtitle() {
    return getTranslation("mfa.confirm.subtitle");
  }

  public String getTwoFactorLoginRecoveryHint() {
    return getTranslation("mfa.login.recoveryHint");
  }

  public String getTwoFactorLoginHelpTitle() {
    return getTranslation("mfa.login.help.title");
  }

  public String getTwoFactorLoginRestorePrompt() {
    return getTranslation("mfa.login.restore.prompt");
  }

  public String getTwoFactorRecoverSubtitle() {
    return getTranslation("mfa.recover.subtitle");
  }

  public String getTwoFactorRecoveryPreviousAppHint() {
    return getTranslation("mfa.recover.previousAppHint");
  }

  public String getTwoFactorRecoveryConsumptionHint() {
    return getTranslation("mfa.recover.consumptionHint");
  }

  public String getTwoFactorRecoveryContinue() {
    return getTranslation("mfa.recover.continue");
  }

  public String getSupportHelpTitle() {
    return getTranslation("support.help.title");
  }

  public String getSupportHelpSubtitle() {
    return getTranslation("support.help.subtitle");
  }

  public String getSupportHelpPageHint() {
    return getTranslation("support.help.pageHint");
  }

  public String getSupportRequestPlaceholder() {
    return getTranslation("support.request.placeholder");
  }

  public String getTwoFactorLoginHint() {
    return getTranslation("mfa.login.hint");
  }

  // Einrichtung der Authenticator-App und Codeprüfung
  public String getTwoFactorSetupTitle() {
    return getTranslation("mfa.setup.title");
  }

  public String getTwoFactorRequiredTitle() {
    return getTranslation("mfa.required.title");
  }

  public String getTwoFactorRequiredHint() {
    return getTranslation("mfa.required.hint");
  }

  public String getTwoFactorBeginSetup() {
    return getTranslation("mfa.required.begin");
  }

  public String getTwoFactorRecoverAction() {
    return getTranslation("mfa.recover.action");
  }

  public String getTwoFactorRecoveryCodeLabel() {
    return getTranslation("mfa.recover.code");
  }

  public String getTwoFactorReturnToCode() {
    return getTranslation("mfa.recover.back");
  }

  public String getTwoFactorSetupHint() {
    return getTranslation("mfa.setup.hint");
  }

  public String getTwoFactorSetupSubtitle() {
    return getTranslation("mfa.setup.subtitle");
  }

  public String getTwoFactorStartSetup() {
    return getTranslation("mfa.setup.start");
  }

  public String getTwoFactorScanHint() {
    return getTranslation("mfa.scan.hint");
  }

  public String getTwoFactorAppHint() {
    return getTranslation("mfa.app.hint");
  }

  public String getTwoFactorAppStep() {
    return getTranslation("mfa.setup.appStep");
  }

  public String getTwoFactorCodeStep() {
    return getTranslation("mfa.setup.codeStep");
  }

  public String getTwoFactorQrTab() {
    return getTranslation("mfa.setup.qrTab");
  }

  public String getTwoFactorManualTab() {
    return getTranslation("mfa.setup.manualTab");
  }

  public String getTwoFactorManualHint() {
    return getTranslation("mfa.setup.manualHint");
  }

  public String getTwoFactorSetupCodeLabel() {
    return getTranslation("mfa.setup.codeLabel");
  }

  public String getTwoFactorConfirmationAppTab() {
    return getTranslation("mfa.confirm.appTab");
  }

  public String getTwoFactorAppCompatibility() {
    return getTranslation("mfa.app.compatibility");
  }

  public String getTwoFactorQrAlt() {
    return getTranslation("mfa.qr.alt");
  }

  public String getTwoFactorManualLabel() {
    return getTranslation("mfa.manual.label");
  }

  public String getTwoFactorSetupCodeHint() {
    return getTranslation("mfa.setup.codeHint");
  }

  public String getTwoFactorManualCopied() {
    return getTranslation("mfa.manual.copied");
  }

  public String getTwoFactorManualCopyFailed() {
    return getTranslation("mfa.manual.copyFailed");
  }

  public String getTwoFactorManualLoadFailed() {
    return getTranslation("mfa.manual.loadFailed");
  }

  public String getTwoFactorEnable() {
    return getTranslation("mfa.enable");
  }

  public String getTwoFactorConfirm() {
    return getTranslation("mfa.confirm");
  }

  public String getTwoFactorEmailCodeHint() {
    return getTranslation("mfa.email.code.hint");
  }

  public String getTwoFactorEmailCodeLabel() {
    return getTranslation("mfa.email.code.label");
  }

  // Ersatzcodes und Download
  public String getTwoFactorRecoveryTitle() {
    return getTranslation("mfa.recovery.title");
  }

  public String getTwoFactorRecoverySubtitle() {
    return getTranslation("mfa.recovery.subtitle");
  }

  public String getTwoFactorRecoveryOnceTitle() {
    return getTranslation("mfa.recovery.once.title");
  }

  public String getTwoFactorRecoveryOnceHint() {
    return getTranslation("mfa.recovery.once.hint");
  }

  public String getTwoFactorRecoveryRestoreTitle() {
    return getTranslation("mfa.recovery.restore.title");
  }

  public String getTwoFactorRecoveryRestoreHint() {
    return getTranslation("mfa.recovery.restore.hint");
  }

  public String getTwoFactorRecoveryStorageTitle() {
    return getTranslation("mfa.recovery.storage.title");
  }

  public String getTwoFactorRecoveryCodesSummary() {
    return getTranslation("mfa.recovery.codesSummary");
  }

  public String getTwoFactorRecoveryExplanation() {
    return getTranslation("mfa.recovery.explanation");
  }

  public String getTwoFactorRecoveryStorage() {
    return getTranslation("mfa.recovery.storage");
  }

  public String getTwoFactorRecoveryReplacement() {
    return getTranslation("mfa.recovery.replacement");
  }

  public String getTwoFactorRecoverySupport() {
    return getTranslation("mfa.recovery.support");
  }

  public String getTwoFactorDocumentAccount() {
    return getTranslation("mfa.document.account");
  }

  public String getTwoFactorDocumentDate() {
    return getTranslation("mfa.document.date");
  }

  public String getTwoFactorDownloadPdf() {
    return getTranslation("mfa.download.pdf");
  }

  public String getTwoFactorDownloadFailed() {
    return getTranslation("mfa.download.failed");
  }

  public String getTwoFactorCodesSavedConfirmation() {
    return getTranslation("mfa.recovery.savedConfirmation");
  }

  public String getTwoFactorCopyCodes() {
    return getTranslation("mfa.copy.codes");
  }

  public String getTwoFactorCodesSaved() {
    return getTranslation("mfa.codes.saved");
  }

  public String getTwoFactorCodeCopied() {
    return getTranslation("mfa.copy.codeSuccess");
  }

  public String getTwoFactorRecoveryLoadFailed() {
    return getTranslation("mfa.recovery.loadFailed");
  }

  public String getTwoFactorCopySuccess() {
    return getTranslation("mfa.copy.success");
  }

  public String getTwoFactorCopyFailed() {
    return getTranslation("mfa.copy.failed");
  }

  public String getTwoFactorUnavailable() {
    return getTranslation("mfa.unavailable");
  }

  public String getTwoFactorSetupRequired() {
    return getTranslation("mfa.setup.required");
  }

  public String getTwoFactorThrottled() {
    return getTranslation("mfa.throttled");
  }

  public String getTwoFactorFailed() {
    return getTranslation("mfa.failed");
  }

  public String getTwoFactorBack() {
    return getTranslation("mfa.back");
  }

  public String getTwoFactorRequestHelp() {
    return getTranslation("mfa.request.help");
  }

  // Zwei-Faktor-Regeln in der Administration
  public String getTwoFactorSettingsTitle() {
    return getTranslation("mfa.settings.title");
  }

  public String getTwoFactorNavigationLabel() {
    return getTranslation("mfa.navigation.label");
  }

  public String getTwoFactorSettingsPageHint() {
    return getTranslation("mfa.settings.page.hint");
  }

  public String getTwoFactorSettingsHint() {
    return getTranslation("mfa.settings.hint");
  }

  public String getTwoFactorAccessGroup() {
    return getTranslation("mfa.settings.group.access");
  }

  public String getTwoFactorAccountGroup() {
    return getTranslation("mfa.settings.group.account");
  }

  public String getTwoFactorMethodsGroup() {
    return getTranslation("mfa.settings.group.methods");
  }

  public String getTwoFactorInactivityGroup() {
    return getTranslation("mfa.settings.group.inactivity");
  }

  public String getTwoFactorRoleGuide() {
    return getTranslation("mfa.settings.guide.title");
  }

  public String getTwoFactorRolePriority() {
    return getTranslation("mfa.settings.guide.priority");
  }

  public String getTwoFactorRolePriorityHint() {
    return getTranslation("mfa.settings.guide.priority.hint");
  }

  public String getTwoFactorExistingFactors() {
    return getTranslation("mfa.settings.guide.factors");
  }

  public String getTwoFactorExistingFactorsHint() {
    return getTranslation("mfa.settings.guide.factors.hint");
  }

  public String getTwoFactorRuleTiming() {
    return getTranslation("mfa.settings.guide.timing");
  }

  public String getTwoFactorRoleScope(String roleName) {
    return getFormattedTranslation("mfa.settings.guide.scope", roleName);
  }

  public String getTwoFactorAdminAccessHelp() {
    return getTranslation("mfa.settings.admin.help");
  }

  public String getTwoFactorModeOff() {
    return getTranslation("mfa.mode.off");
  }

  public String getTwoFactorModeEnrolled() {
    return getTranslation("mfa.mode.enrolled");
  }

  public String getTwoFactorModeRequired() {
    return getTranslation("mfa.mode.required");
  }

  public String getTwoFactorRulesHint() {
    return getTranslation("mfa.rules.hint");
  }

  public String getTwoFactorRuleLogin() {
    return getTranslation("mfa.rule.login");
  }

  public String getTwoFactorRuleAdministration() {
    return getTranslation("mfa.rule.administration");
  }

  public String getTwoFactorRulePassword() {
    return getTranslation("mfa.rule.password");
  }

  public String getTwoFactorRuleEmail() {
    return getTranslation("mfa.rule.email");
  }

  public String getTwoFactorRuleLink() {
    return getTranslation("mfa.rule.link");
  }

  public String getTwoFactorRuleUnlink() {
    return getTranslation("mfa.rule.unlink");
  }

  public String getTwoFactorRuleFactor() {
    return getTranslation("mfa.rule.factor");
  }

  public String getTwoFactorRuleRecovery() {
    return getTranslation("mfa.rule.recovery");
  }

  public String getTwoFactorRuleConfiguration() {
    return getTranslation("mfa.rule.configuration");
  }

  public String getTwoFactorRuleHelp() {
    return String.join(" ", twoFactorRuleHelpItems());
  }

  public List<String> twoFactorRuleHelpItems() {
    return List.of(
        getTranslation("mfa.rule.help.off"),
        getTranslation("mfa.rule.help.enrolled"),
        getTranslation("mfa.rule.help.required"),
        getTranslation("mfa.rule.help.confirmation"));
  }

  // Inaktivität, Entwicklungsoptionen und Speichern
  public String getTwoFactorInactivityEnabled() {
    return getTranslation("mfa.inactivity.enabled");
  }

  public String getTwoFactorInactivityDuration() {
    return getTranslation("mfa.inactivity.duration");
  }

  public String getTwoFactorInactivityHelp() {
    return getTwoFactorInactivityDescription() + " " + getTwoFactorInactivityRange();
  }

  public String getTwoFactorInactivityDescription() {
    return getTranslation("mfa.inactivity.help");
  }

  public String getTwoFactorInactivityRange() {
    return getTranslation("mfa.inactivity.range");
  }

  public String getTwoFactorDevelopmentTitle() {
    return getTranslation("mfa.development.title");
  }

  public String getTwoFactorDevelopmentPause() {
    return getTranslation("mfa.development.pause");
  }

  public String getTwoFactorDevelopmentGoogle() {
    return getTranslation("mfa.development.google");
  }

  public String getTwoFactorDevelopmentWarning() {
    return getTranslation("mfa.development.warning");
  }

  public String getTwoFactorSaveHint() {
    return getTranslation("mfa.save.hint");
  }

  public String getTwoFactorSaved() {
    return getTranslation("mfa.saved");
  }

  public String getTwoFactorConflict() {
    return getTranslation("mfa.conflict");
  }

  // Zwei-Faktor-Verwaltung im eigenen Konto
  public String getTwoFactorAccountHint() {
    return getTranslation("mfa.account.hint");
  }

  public String getTwoFactorEnrolled() {
    return getTranslation("mfa.enrolled");
  }

  public String getTwoFactorNotEnrolled() {
    return getTranslation("mfa.not.enrolled");
  }

  public String getTwoFactorSetupAction() {
    return getTranslation("mfa.setup.action");
  }

  public String getTwoFactorReplaceAction() {
    return getTranslation("twoFactor.replaceAction");
  }

  public String getTwoFactorDisableAction() {
    return getTranslation("mfa.disable.action");
  }

  public String getTwoFactorRegenerateAction() {
    return getTranslation("mfa.regenerate.action");
  }

  public String getTwoFactorIndependentAccessRequired() {
    return getTranslation("twoFactor.independentAccessRequired");
  }

  public String getTwoFactorDisableHint() {
    return getTranslation("mfa.disable.hint");
  }

  public String getTwoFactorDisabled() {
    return getTranslation("mfa.disabled");
  }

  // Zwei-Faktor-E-Mails, Sicherheitshinweise und Codeeingabe
  public String getTwoFactorDisabledSubject() {
    return getTranslation("mfa.disabled.subject");
  }

  public String getTwoFactorDisabledExplanation() {
    return getTranslation("mfa.disabled.explanation");
  }

  public String getTwoFactorRenewedSubject() {
    return getTranslation("mfa.renewed.subject");
  }

  public String getTwoFactorRenewedExplanation() {
    return getTranslation("mfa.renewed.explanation");
  }

  public String getTwoFactorEmailSubject() {
    return getTranslation("mfa.email.subject");
  }

  public String getTwoFactorEmailExplanation() {
    return getTranslation("mfa.email.explanation");
  }

  public String getTwoFactorEmailNotice() {
    return getTranslation("mfa.email.notice");
  }

  public String getTwoFactorEnabledSubject() {
    return getTranslation("mfa.enabled.subject");
  }

  public String getTwoFactorEnabledExplanation() {
    return getTranslation("mfa.enabled.explanation");
  }

  public String getTwoFactorSecurityNotice() {
    return getTranslation("mfa.security.notice");
  }

  public String getTwoFactorActionHint() {
    return getTranslation("mfa.action.hint");
  }

  public String getTwoFactorActionRecoveryHint() {
    return getTranslation("mfa.action.recoveryHint");
  }

  public String getTwoFactorCodePlaceholder() {
    return getTranslation("mfa.code.placeholder");
  }

  public String getTwoFactorInvalidCode() {
    return getTranslation("mfa.code.invalid");
  }

  // Anmeldung mit externen Anbietern
  public String getExternalGoogle() {
    return getTranslation("external.google");
  }

  public String getExternalMicrosoft() {
    return getTranslation("external.microsoft");
  }

  public String getExternalOr() {
    return getTranslation("external.or");
  }

  public String getExternalPanelKicker() {
    return getTranslation("external.panel.kicker");
  }

  public String getExternalPanelTitle() {
    return getTranslation("external.panel.title");
  }

  public String getExternalPanelHint() {
    return getTranslation("external.panel.hint");
  }

  public String getExternalContinueGoogle() {
    return getTranslation("external.google.continue");
  }

  public String getExternalContinueMicrosoft() {
    return getTranslation("external.microsoft.continue");
  }

  public String getExternalCompleteTitle() {
    return getTranslation("external.complete.title");
  }

  public String getExternalCompleteHint() {
    return getTranslation("external.complete.hint");
  }

  public String getExternalError() {
    return getTranslation("external.error");
  }

  public String getExternalProofFreshRequired() {
    return getTranslation("external.proof.fresh_required");
  }

  public String getExternalProofIdentityMismatch() {
    return getTranslation("external.proof.identity_mismatch");
  }

  public String getExternalProofUnavailable() {
    return getTranslation("external.proof.unavailable");
  }

  public String getExternalVerifyEmail() {
    return getTranslation("external.verify.email");
  }

  public String getExternalConnect(String providerName) {
    return getFormattedTranslation("external.connect", providerName);
  }

  public String getExternalDisconnect(String providerName) {
    return getFormattedTranslation("external.disconnect", providerName);
  }

  public String getExternalProof() {
    return getTranslation("external.proof");
  }

  public String getExternalProofWith(String providerName) {
    return getFormattedTranslation("external.proof.with", providerName);
  }

  public String getExternalProofHint() {
    return getTranslation("external.proof.hint");
  }

  public String getExternalPasswordProofHint() {
    return getTranslation("external.proof.passwordHint");
  }

  public String getExternalProofReady() {
    return getTranslation("external.proof.ready");
  }

  public String getExternalPasswordSetup() {
    return getTranslation("external.password.setup");
  }

  public String getExternalDisconnectHint() {
    return getTranslation("external.disconnect.hint");
  }

  public String getExternalDisconnected() {
    return getTranslation("external.disconnected");
  }

  public String getExternalDisconnectedHint() {
    return getTranslation("external.disconnected.hint");
  }

  public String getExternalDisconnectedSignIn() {
    return getTranslation("external.disconnected.signIn");
  }

  public String getAccountSecurityPortalAccess() {
    return getTranslation("account.security.portalAccess");
  }

  public String getAccountSecurityNoProviders() {
    return getTranslation("account.security.noProviders");
  }

  public String getExternalConnected() {
    return getTranslation("external.connected");
  }

  public String getExternalNotConnected() {
    return getTranslation("external.notConnected");
  }

  public String getExternalConnectedHint() {
    return getTranslation("external.connected.hint");
  }

  public String getExternalNotConnectedHint() {
    return getTranslation("external.notConnected.hint");
  }

  public String getExternalLastMethod() {
    return getTranslation("external.lastMethod");
  }

  public String getExternalProviderUnavailable() {
    return getTranslation("external.providerUnavailable");
  }

  public String getExternalContinueAction() {
    return getTranslation("external.continueAction");
  }

  public String getExternalDraftExpired() {
    return getTranslation("external.draft.expired");
  }

  // Startseite und Portalnavigation
  public String getHomeTitle() {
    return getTranslation("homeView.title");
  }

  public String getHomeDescription() {
    return getTranslation("homeView.description");
  }

  public String getPortalHomeIntroduction() {
    return getTranslation("portal.home.introduction");
  }

  public String getPortalHomeKicker() {
    return getTranslation("portal.home.kicker");
  }

  public String getPortalHomeWelcome() {
    return getTranslation("portal.home.welcome");
  }

  public String getPortalHomeDescription() {
    return getTranslation("portal.home.description");
  }

  public String getPortalHomeNotice() {
    return getTranslation("portal.home.notice");
  }

  public String getHomeOverview() {
    return getTranslation("homeView.overview");
  }

  // Gemeinsame Formulare und Copyright-Konfiguration
  public String getAccountEmailPlaceholder() {
    return getTranslation("account.form.email.placeholder");
  }

  public String getAccountConfirmEmailPlaceholder() {
    return getTranslation("account.form.confirm_email.placeholder");
  }

  public String getAccountPasswordPlaceholder() {
    return getTranslation("account.form.password.placeholder");
  }

  public String getAccountConfirmPasswordPlaceholder() {
    return getTranslation("account.form.confirm_password.placeholder");
  }

  public String getAccountNewPasswordPlaceholder() {
    return getTranslation("account.form.new_password.placeholder");
  }

  public String getAccountFirstNamePlaceholder() {
    return getTranslation("account.form.first_name.placeholder");
  }

  public String getAccountLastNamePlaceholder() {
    return getTranslation("account.form.last_name.placeholder");
  }

  public String getAccountGenderPlaceholder() {
    return getTranslation("account.form.gender.placeholder");
  }

  public String getAdminConfigEntryCopyrightText() {
    return getTranslation("adminView.config.entry.copyright_text");
  }

  public String getAdminConfigEntryCopyrightTextTooltip() {
    return getTranslation("adminView.config.entry.copyright_text.tooltip");
  }

  // Anmeldung
  public String getLoginTitle() {
    return getTranslation("loginView.title");
  }

  public String getLoginCreateAccountQuestion() {
    return getTranslation("loginView.create_account_question");
  }

  public String getLoginCreateAccountLink() {
    return getTranslation("loginView.create_account_link");
  }

  public String getLoginPasswordField() {
    return getTranslation("loginView.password_field");
  }

  public String getLoginRememberMeCheckbox() {
    return getTranslation("loginView.remember_me_checkbox");
  }

  public String getLoginForgotPasswordLink() {
    return getTranslation("loginView.forgot_password_link");
  }

  public String getLoginFailedMessage() {
    return getTranslation("loginView.login.failed_message");
  }

  public String getLoginThrottledMessage() {
    return getTranslation("login.throttled.message");
  }

  public String getLoginSessionLimitMessage() {
    return getTranslation("login.sessions.limit.message");
  }

  public String getLoginUnavailableMessage() {
    return getTranslation("login.unavailable.message");
  }

  public String getLoginSuccessMessage() {
    return getTranslation("loginView.login.success_message");
  }

  // Passwort vergessen
  public String getForgotPasswordTitle() {
    return getTranslation("forgotPasswordView.title");
  }

  public String getForgotPasswordText() {
    return getTranslation("forgotPasswordView.text");
  }

  public String getForgotPasswordSuccessMessage() {
    return getTranslation("forgotPasswordView.request_message");
  }

  public String getResetPasswordTechnicalErrorMessage() {
    return getTranslation("resetPasswordView.technical_error_message");
  }

  // E-Mail-Betreffzeilen
  public String getEmailResetPasswordSubject() {
    return getTranslation("email.reset_password.subject");
  }

  public String getEmailVerificationSubject() {
    return getTranslation("email.verification.subject");
  }

  // Registrierung
  public String getRegistrationTitle() {
    return getTranslation("registrationView.title");
  }

  public String getRegistrationLoginQuestion() {
    return getTranslation("registrationView.login_question");
  }

  public String getRegistrationLoginLink() {
    return getTranslation("registrationView.login_link");
  }

  public String getRegistrationFirstNameField() {
    return getTranslation("registrationView.first_name_field");
  }

  public String getRegistrationLastNameField() {
    return getTranslation("registrationView.last_name_field");
  }

  public String getRegistrationPasswordField() {
    return getTranslation("registrationView.password_field");
  }

  public String getRegistrationGenderCombobox() {
    return getTranslation("registrationView.gender_combobox");
  }

  public String getRegistrationBirthDateField() {
    return getTranslation("registrationView.birth_date_field");
  }

  public String getRegistrationBirthDatePlaceholder() {
    return getTranslation("registrationView.birth_date_placeholder");
  }

  public String getRegistrationSuccessMessage(String registrationEmailAddress) {
    return getFormattedTranslation(
        "registrationView.registration.success_message", registrationEmailAddress);
  }

  public String getRegistrationUnavailableMessage() {
    return getTranslation("registrationView.registration.unavailable_message");
  }

  // Kontoaktivierung
  public String getVerificationSuccessTitle() {
    return getTranslation("verificationView.success.title");
  }

  public String getVerificationSuccessText() {
    return getTranslation("verificationView.success.text");
  }

  public String getVerificationExpiredTitle() {
    return getTranslation("verificationView.expired.title");
  }

  public String getVerificationExpiredText1() {
    return getTranslation("verificationView.expired.text1");
  }

  public String getVerificationExpiredSuccessMessage() {
    return getTranslation("verificationView.expired.success_message");
  }

  public String getVerificationNotFoundTitle() {
    return getTranslation("verificationView.not_found.title");
  }

  public String getVerificationNotFoundText1() {
    return getTranslation("verificationView.not_found.text1");
  }

  public String getVerificationNotFoundText2() {
    return getTranslation("verificationView.not_found.text2");
  }

  public String getVerificationRequestActivationLink() {
    return getTranslation("verificationView.request_activation_link");
  }

  public String getVerificationNotFoundSuccessMessage() {
    return getTranslation("verificationView.not_found.success_message");
  }

  // Passwort zurücksetzen
  public String getResetPasswordInvalidTitle() {
    return getTranslation("resetPasswordView.invalid.title");
  }

  public String getResetPasswordExpiredTitle() {
    return getTranslation("resetPasswordView.expired.title");
  }

  public String getResetPasswordExpiredText() {
    return getTranslation("resetPasswordView.expired.text");
  }

  public String getResetPasswordInvalidText() {
    return getTranslation("resetPasswordView.invalid.text");
  }

  public String getResetPasswordTitle() {
    return getTranslation("resetPasswordView.reset.title");
  }

  public String getResetPasswordNewPasswordField() {
    return getTranslation("resetPasswordView.reset.new_password_field");
  }

  public String getResetPasswordConfirmField() {
    return getTranslation("resetPasswordView.reset.confirm_field");
  }

  public String getResetPasswordSuccessMessage() {
    return getTranslation("resetPasswordView.reset.success_message");
  }

  // Allgemeine Schaltflächen
  public String getBaseDetailsButton() {
    return getTranslation("base.button.details");
  }

  public String getBaseLoginButton() {
    return getTranslation("base.login_button");
  }

  public String getBaseLogoutButton() {
    return getTranslation("base.logout_button");
  }

  public String getBaseHomeButton() {
    return getTranslation("base.home_button");
  }

  public String getBaseResendButton() {
    return getTranslation("base.resend_button");
  }

  public String getBaseRegistrationButton() {
    return getTranslation("base.registration_button");
  }

  public String getBaseNotifications() {
    return getTranslation("base.notifications");
  }

  public String getBaseManageAccountButton() {
    return getTranslation("base.manage_account_button");
  }

  public String getBaseManageApplicationButton() {
    return getTranslation("base.manage_application_button");
  }

  // Allgemeine Status- und Fehlermeldungen
  public String getBaseSuccessTitle() {
    return getTranslation("base.success.title");
  }

  public String getBaseFailedTitle() {
    return getTranslation("base.failed.title");
  }

  public String getBaseUserUpdateConflict() {
    return getTranslation("base.user.update.conflict");
  }

  public String getBaseUserStatusFailed() {
    return getTranslation("base.user.status.failed");
  }

  public String getBaseFailedMessage() {
    return getTranslation("base.failed.message");
  }

  public String getBaseErrorAdminAccessDenied() {
    return getTranslation("base.error.admin_access_denied");
  }

  public String getBaseErrorAccessDenied() {
    return getTranslation("base.error.access_denied");
  }

  public String getBaseErrorPathNotFound() {
    return getTranslation("base.error.path_not_found");
  }

  // Allgemeine Validierungsmeldungen
  public String getBaseValidationRequiredMessage() {
    return getTranslation("base.validation.required_message");
  }

  public String getBaseValidationEmailInvalidMessage() {
    return getTranslation("base.validation.email_invalid_message");
  }

  public String getBaseValidationPasswordInvalidMessage() {
    return getTranslation("base.validation.password_invalid_message");
  }

  public String getBaseValidationPasswordConfirmMessage() {
    return getTranslation("base.validation.password_confirm_message");
  }

  public String getBaseValidationEmailExistsMessage() {
    return getTranslation("base.validation.email_exists_message");
  }

  public String getBaseValidationEmailConfirmMessage() {
    return getTranslation("base.validation.email_confirm_message");
  }

  public String getBaseValidationDateInvalidMessage() {
    return getTranslation("base.validation.date_invalid_message");
  }

  public String getBaseValidationBirthDatePastMessage() {
    return getTranslation("base.validation.birth_date_past_message");
  }

  // Benutzerstatus
  public String getUserStatusOffline() {
    return getTranslation("userStatus.offline");
  }

  public String getUserStatusOfflineDescription() {
    return getTranslation("userStatus.offline.description");
  }

  public String getUserStatusAbsent() {
    return getTranslation("userStatus.absent");
  }

  public String getUserStatusAbsentDescription() {
    return getTranslation("userStatus.absent.description");
  }

  public String getUserStatusEmployed() {
    return getTranslation("userStatus.employed");
  }

  public String getUserStatusEmployedDescription() {
    return getTranslation("userStatus.employed.description");
  }

  public String getUserStatusAvailable() {
    return getTranslation("userStatus.available");
  }

  public String getUserStatusAvailableDescription() {
    return getTranslation("userStatus.available.description");
  }

  // Admin-Navigation
  public String getAdminHeaderTitle() {
    return getTranslation("adminView.header.title");
  }

  public String getAdminNavDashboard() {
    return getTranslation("adminView.nav.dashboard");
  }

  public String getAdminNavToggleChildren() {
    return getTranslation("adminView.nav.toggle_children");
  }

  public String getAdminNavConfiguration() {
    return getTranslation("adminView.nav.configuration");
  }

  public String getAdminNavMailTest() {
    return getTranslation("adminView.nav.mail_test");
  }

  // Konfigurationsgruppen und Aufbewahrung von Mitteilungen
  public String getAdminConfigGroupNotifications() {
    return getTranslation("adminView.config.group.notifications");
  }

  public String getAdminConfigGroupNotificationsDescription() {
    return getTranslation("adminView.config.group.notifications.description");
  }

  public String getAdminConfigEntryNotificationInboxRetention() {
    return getTranslation("adminView.config.entry.notification_inbox_retention");
  }

  public String getAdminConfigEntryNotificationHistoryRetention() {
    return getTranslation("adminView.config.entry.notification_history_retention");
  }

  public String getAdminConfigEntryNotificationInboxRetentionTooltip() {
    return getTranslation("adminView.config.entry.notification_inbox_retention.tooltip");
  }

  public String getAdminConfigEntryNotificationHistoryRetentionTooltip() {
    return getTranslation("adminView.config.entry.notification_history_retention.tooltip");
  }

  public String getAdminConfigGroupGeneral() {
    return getTranslation("adminView.config.group.general");
  }

  public String getAdminConfigGroupSecurity() {
    return getTranslation("adminView.config.group.security");
  }

  public String getAdminConfigGroupTokens() {
    return getTranslation("adminView.config.group.tokens");
  }

  public String getAdminConfigGroupUserStatus() {
    return getTranslation("adminView.config.group.user_status");
  }

  public String getAdminConfigGroupEmailSender() {
    return getTranslation("adminView.config.group.email_sender");
  }

  public String getAdminConfigGroupSmtp() {
    return getTranslation("adminView.config.group.smtp");
  }

  public String getAdminConfigGroupQueue() {
    return getTranslation("adminView.config.group.queue");
  }

  // Admin-Übersicht
  public String getAdminDashboardHeroEyebrow() {
    return getTranslation("adminView.dashboard.hero_eyebrow");
  }

  public String getAdminDashboardHeroTitle() {
    return getTranslation("adminView.dashboard.hero_title");
  }

  public String getAdminDashboardHeroDescription() {
    return getTranslation("adminView.dashboard.hero_description");
  }

  public String getAdminDashboardFutureHint() {
    return getTranslation("adminView.dashboard.future_hint");
  }

  public String getAdminDashboardShortcuts() {
    return getTranslation("adminView.dashboard.shortcuts");
  }

  public String getAdminDashboardShortcutsHint() {
    return getTranslation("adminView.dashboard.shortcuts_hint");
  }

  public String getAdminDashboardOpen() {
    return getTranslation("adminView.dashboard.open");
  }

  public String getAdminDashboardTitle() {
    return getTranslation("adminView.dashboard.title");
  }

  public String getAdminDashboardDescription() {
    return getTranslation("adminView.dashboard.description");
  }

  // E-Mail-Test
  public String getAdminMailTitle() {
    return getTranslation("adminView.mail.title");
  }

  public String getAdminMailDescription() {
    return getTranslation("adminView.mail.description");
  }

  public String getAdminMailCardTitle() {
    return getTranslation("adminView.mail.card_title");
  }

  public String getAdminMailCardDescription() {
    return getTranslation("adminView.mail.card_description");
  }

  public String getAdminMailCardBadge() {
    return getTranslation("adminView.mail.card_badge");
  }

  public String getAdminMailSendHint() {
    return getTranslation("adminView.mail.send_hint");
  }

  public String getAdminMailRecipientHint() {
    return getTranslation("adminView.mail.recipient_hint");
  }

  public String getAdminMailSubjectHint() {
    return getTranslation("adminView.mail.subject_hint");
  }

  public String getAdminMailBodyHint() {
    return getTranslation("adminView.mail.body_hint");
  }

  public String getAdminMailSend() {
    return getTranslation("adminView.mail.send");
  }

  public String getAdminMailRecipient() {
    return getTranslation("adminView.mail.recipient");
  }

  public String getAdminMailSubject() {
    return getTranslation("adminView.mail.subject");
  }

  public String getAdminMailBody() {
    return getTranslation("adminView.mail.body");
  }

  public String getAdminMailDefaultSubject() {
    return getTranslation("adminView.mail.default_subject");
  }

  public String getAdminMailDefaultBody() {
    return getTranslation("adminView.mail.default_body");
  }

  public String getAdminMailValidationRecipient() {
    return getTranslation("adminView.mail.validation.recipient");
  }

  public String getAdminMailValidationSubject() {
    return getTranslation("adminView.mail.validation.subject");
  }

  public String getAdminMailValidationBody() {
    return getTranslation("adminView.mail.validation.body");
  }

  public String getAdminMailSuccess() {
    return getTranslation("adminView.mail.success");
  }

  public String getAdminMailError() {
    return getTranslation("adminView.mail.error");
  }

  // Konfigurationsansicht und Gruppenbeschreibungen
  public String getAdminConfigTitle() {
    return getTranslation("adminView.config.title");
  }

  public String getAdminConfigDescription() {
    return getTranslation("adminView.config.description");
  }

  public String getAdminConfigGroupGeneralDescription() {
    return getTranslation("adminView.config.group.general.description");
  }

  public String getAdminConfigGroupSecurityDescription() {
    return getTranslation("adminView.config.group.security.description");
  }

  public String getAdminConfigGroupTokensDescription() {
    return getTranslation("adminView.config.group.tokens.description");
  }

  public String getAdminConfigGroupUserStatusDescription() {
    return getTranslation("adminView.config.group.user_status.description");
  }

  public String getAdminConfigGroupEmailSenderDescription() {
    return getTranslation("adminView.config.group.email_sender.description");
  }

  public String getAdminConfigGroupSmtpDescription() {
    return getTranslation("adminView.config.group.smtp.description");
  }

  public String getAdminConfigGroupQueueDescription() {
    return getTranslation("adminView.config.group.queue.description");
  }

  public String getAdminConfigGroupDescription(AdminConfigGroup configGroup) {
    return switch (configGroup) {
      case GENERAL -> getAdminConfigGroupGeneralDescription();
      case SECURITY -> getAdminConfigGroupSecurityDescription();
      case EXTERNAL_LOGIN -> getProviderSettingsDescription();
      case TWO_FACTOR -> getTwoFactorSettingsHint();
      case TOKENS -> getAdminConfigGroupTokensDescription();
      case USER_STATUS -> getAdminConfigGroupUserStatusDescription();
      case EMAIL_SENDER -> getAdminConfigGroupEmailSenderDescription();
      case SMTP -> getAdminConfigGroupSmtpDescription();
      case NOTIFICATIONS -> getAdminConfigGroupNotificationsDescription();
      case QUEUE -> getAdminConfigGroupQueueDescription();
    };
  }

  public String getAdminConfigSettingsCount(int settingCount) {
    return getFormattedTranslation(
        settingCount == 1
            ? "adminView.config.settings_count.one"
            : "adminView.config.settings_count.other",
        settingCount);
  }

  public String getAdminConfigSaveHint() {
    return getTranslation("adminView.config.save_hint");
  }

  public String getAdminConfigSave() {
    return getTranslation("adminView.config.save");
  }

  public String getAdminConfigReload() {
    return getTranslation("adminView.config.reload");
  }

  // Eingaberegeln und Standardwerte
  public String getAdminConfigRuleOptional() {
    return getTranslation("adminView.config.rule.optional");
  }

  public String getAdminConfigRuleEmptyUsesDefault() {
    return getTranslation("adminView.config.rule.empty_uses_default");
  }

  public String getAdminConfigRuleBoolean() {
    return getTranslation("adminView.config.rule.boolean");
  }

  public String getAdminConfigRuleHost() {
    return getTranslation("adminView.config.rule.host");
  }

  public String getAdminConfigRuleSslTrust() {
    return getTranslation("adminView.config.rule.ssl_trust");
  }

  public String getAdminConfigRuleDurationNoMinimum() {
    return getTranslation("adminView.config.rule.duration_no_minimum");
  }

  public String getAdminConfigDefaultEmpty() {
    return getTranslation("adminView.config.default_empty");
  }

  public String getAdminConfigDefaultConfidential() {
    return getTranslation("adminView.config.default_confidential");
  }

  public String getAdminConfigPlaceholderEmpty() {
    return getTranslation("adminView.config.placeholder.empty");
  }

  public String getAdminConfigRuleInteger(int minimumValue, int maximumValue) {
    return getFormattedTranslation(
        "adminView.config.rule.integer",
        Integer.toString(minimumValue),
        Integer.toString(maximumValue));
  }

  public String getAdminConfigInputRules() {
    return getTranslation("adminView.config.input_rules");
  }

  public String getAdminConfigDefault() {
    return getTranslation("adminView.config.default");
  }

  // Dauerfelder und lokalisierte Zahlen
  public String getAdminConfigDurationIncrease() {
    return getTranslation("adminView.config.duration.increase");
  }

  public String getAdminConfigDurationDecrease() {
    return getTranslation("adminView.config.duration.decrease");
  }

  public String getAdminConfigDurationNumberRule() {
    return getTranslation("adminView.config.duration.number_rule");
  }

  public String getAdminConfigDurationUnit() {
    return getTranslation("adminView.config.duration.unit");
  }

  public String getAdminConfigDurationUnitSeconds() {
    return getTranslation("adminView.config.duration.unit.seconds");
  }

  public String getAdminConfigDurationUnitMinutes() {
    return getTranslation("adminView.config.duration.unit.minutes");
  }

  public String getAdminConfigDurationUnitHours() {
    return getTranslation("adminView.config.duration.unit.hours");
  }

  public String getAdminConfigDurationUnitDays() {
    return getTranslation("adminView.config.duration.unit.days");
  }

  public String getAdminConfigDurationCurrent() {
    return getTranslation("adminView.config.duration.current");
  }

  public String getAdminConfigDurationEquivalent() {
    return getTranslation("adminView.config.duration.equivalent");
  }

  public String getAdminConfigDurationInvalid() {
    return getTranslation("adminView.config.duration.invalid");
  }

  public String getAdminConfigDurationUnitHint() {
    return getTranslation("adminView.config.duration.unit_hint");
  }

  public String getAdminConfigDurationSeconds(Number secondCount) {
    return formatDurationTranslation("seconds", secondCount);
  }

  public String getAdminConfigDurationMinutes(Number minuteCount) {
    return formatDurationTranslation("minutes", minuteCount);
  }

  public String getAdminConfigDurationHours(Number hourCount) {
    return formatDurationTranslation("hours", hourCount);
  }

  public String getAdminConfigDurationDays(Number dayCount) {
    return formatDurationTranslation("days", dayCount);
  }

  protected Locale formattingLocale() {
    return Locale.getDefault();
  }

  private String formatDurationTranslation(String durationUnitKeyPart, Number configuredDuration) {
    BigDecimal durationAmount = new BigDecimal(configuredDuration.toString());
    NumberFormat durationNumberFormat = NumberFormat.getNumberInstance(formattingLocale());
    durationNumberFormat.setMaximumFractionDigits(Math.max(9, durationAmount.scale()));

    String translationKey =
        "adminView.config.duration."
            + durationUnitKeyPart
            + (durationAmount.compareTo(BigDecimal.ONE) == 0 ? ".one" : ".many");

    return getFormattedTranslation(translationKey, durationNumberFormat.format(durationAmount));
  }

  public String getAdminConfigDurationRule(int maximumSeconds) {
    return getFormattedTranslation("adminView.config.duration.rule", maximumSeconds);
  }

  // Konfigurationshilfe und Rückmeldungen beim Speichern
  public String getAdminConfigHelpTitle() {
    return getTranslation("adminView.config.help.title");
  }

  public String getAdminConfigHelpDescription() {
    return getTranslation("adminView.config.help.description");
  }

  public String getAdminConfigHelpPlaceholders() {
    return getTranslation("adminView.config.help.placeholders");
  }

  public String getAdminConfigHelpYear() {
    return getTranslation("adminView.config.help.year");
  }

  public String getAdminConfigHelpServiceName() {
    return getTranslation("adminView.config.help.service_name");
  }

  public String getAdminConfigSaveSuccess() {
    return getTranslation("adminView.config.save_success");
  }

  public String getAdminConfigReloadSuccess() {
    return getTranslation("adminView.config.reload_success");
  }

  public String getAdminConfigRestartRequired() {
    return getTranslation("adminView.config.restart_required");
  }

  public String getAdminConfigRestartSaved() {
    return getTranslation("adminView.config.restart_saved");
  }

  public String getAdminConfigRestartDetail() {
    return getTranslation("adminView.config.restart_detail");
  }

  // Zeitpunkt und Reichweite von Konfigurationsänderungen
  public String getAdminConfigEffectiveTitle() {
    return getTranslation("adminView.config.effective_title");
  }

  public String getAdminConfigEntryLoginAttemptWindow() {
    return getTranslation("adminView.config.entry.login_attempt_window_seconds");
  }

  public String getAdminConfigEntryLoginAttemptWindowTooltip() {
    return getTranslation("adminView.config.entry.login_attempt_window_seconds.tooltip");
  }

  public String getAdminConfigEntryEmailQueueRetryDelay() {
    return getTranslation("adminView.config.entry.email_queue_retry_delay_seconds");
  }

  public String getAdminConfigEntryEmailQueueRetryDelayTooltip() {
    return getTranslation("adminView.config.entry.email_queue_retry_delay_seconds.tooltip");
  }

  public String getAdminConfigEffectiveServiceName() {
    return getTranslation("adminView.config.effective_service_name");
  }

  public String getAdminConfigEffectiveCopyright() {
    return getTranslation("adminView.config.effective_copyright");
  }

  public String getAdminConfigEffectiveBaseUrl() {
    return getTranslation("adminView.config.effective_base_url");
  }

  public String getAdminConfigEffectiveRestartRemembered() {
    return getTranslation("adminView.config.effective_restart_remembered");
  }

  public String getAdminConfigEffectiveMaintenance() {
    return getTranslation("adminView.config.effective_maintenance");
  }

  public String getAdminConfigEffectiveAutoAway() {
    return getTranslation("adminView.config.effective_auto_away");
  }

  public String getAdminConfigEffectiveInboxRetention() {
    return getTranslation("adminView.config.effective_inbox_retention");
  }

  public String getAdminConfigEffectiveHistoryRetention() {
    return getTranslation("adminView.config.effective_history_retention");
  }

  public String getAdminConfigEffectiveCleanupExisting() {
    return getTranslation("adminView.config.effective_cleanup_existing");
  }

  public String getAdminConfigEffectiveTokenRetention() {
    return getTranslation("adminView.config.effective_token_retention");
  }

  public String getAdminConfigEffectiveMailFrom() {
    return getTranslation("adminView.config.effective_mail_from");
  }

  public String getAdminConfigEffectiveMailAdmin() {
    return getTranslation("adminView.config.effective_mail_admin");
  }

  public String getAdminConfigEffectiveQueueCurrentBatch() {
    return getTranslation("adminView.config.effective_queue_current_batch");
  }

  public String getAdminConfigEffectiveProviderClient() {
    return getTranslation("adminView.config.effective_provider_client");
  }

  public String getAdminConfigEffectiveProviderSecret() {
    return getTranslation("adminView.config.effective_provider_secret");
  }

  public String getAdminConfigEffectiveProviderEnabled() {
    return getTranslation("adminView.config.effective_provider_enabled");
  }

  public String getAdminConfigEffectiveProviderExisting() {
    return getTranslation("adminView.config.effective_provider_existing");
  }

  public String getAdminConfigEffectiveTwoFactorRule() {
    return getTranslation("adminView.config.effective_two_factor_rule");
  }

  public String getAdminConfigEffectiveTwoFactorInactivitySwitch() {
    return getTranslation("adminView.config.effective_two_factor_inactivity_switch");
  }

  public String getAdminConfigEffectiveTwoFactorInactivityDuration() {
    return getTranslation("adminView.config.effective_two_factor_inactivity_duration");
  }

  public String getAdminConfigEffectiveTwoFactorExisting() {
    return getTranslation("adminView.config.effective_two_factor_existing");
  }

  public String getAdminConfigEffectiveSessionNew() {
    return getTranslation("adminView.config.effective_session_new");
  }

  public String getAdminConfigEffectiveSessionExisting() {
    return getTranslation("adminView.config.effective_session_existing");
  }

  public String getAdminConfigEffectiveLoginAttempt() {
    return getTranslation("adminView.config.effective_login_attempt");
  }

  public String getAdminConfigEffectiveLoginBlock() {
    return getTranslation("adminView.config.effective_login_block");
  }

  public String getAdminConfigEffectiveSessionLimit() {
    return getTranslation("adminView.config.effective_session_limit");
  }

  public String getAdminConfigEffectiveSessionLimitExisting() {
    return getTranslation("adminView.config.effective_session_limit_existing");
  }

  public String getAdminConfigEffectiveTokenNew() {
    return getTranslation("adminView.config.effective_token_new");
  }

  public String getAdminConfigEffectiveTokenExisting() {
    return getTranslation("adminView.config.effective_token_existing");
  }

  public String getAdminConfigEffectiveNextQueueRun() {
    return getTranslation("adminView.config.effective_next_queue_run");
  }

  public String getAdminConfigEffectiveQueueInFlight() {
    return getTranslation("adminView.config.effective_queue_in_flight");
  }

  public String getAdminConfigEffectiveWaitingRetries() {
    return getTranslation("adminView.config.effective_waiting_retries");
  }

  public String getAdminConfigEffectiveRememberNew() {
    return getTranslation("adminView.config.effective_remember_new");
  }

  public String getAdminConfigEffectiveRememberExisting() {
    return getTranslation("adminView.config.effective_remember_existing");
  }

  public String getAdminConfigEffectiveRememberShortened() {
    return getTranslation("adminView.config.effective_remember_shortened");
  }

  public String getAdminConfigEffectiveMailTransport() {
    return getTranslation("adminView.config.effective_mail_transport");
  }

  // Bezeichnungen der Konfigurationseinträge
  public String getAdminConfigEntryServiceName() {
    return getTranslation("adminView.config.entry.service_name");
  }

  public String getAdminConfigEntryBaseUrl() {
    return getTranslation("adminView.config.entry.base_url");
  }

  public String getAdminConfigEntryRememberMeDuration() {
    return getTranslation("adminView.config.entry.remember_me_duration");
  }

  public String getAdminConfigEntryRememberMeSecretKey() {
    return getTranslation("adminView.config.entry.remember_me_secret_key");
  }

  public String getAdminConfigEntryUserAutoAbsentTimeout() {
    return getTranslation("adminView.config.entry.user_auto_absent_timeout");
  }

  public String getAdminConfigEntryLoginAttemptsLimit() {
    return getTranslation("adminView.config.entry.login_attempts_limit");
  }

  public String getAdminConfigEntryMaxSessionsPerUser() {
    return getTranslation("adminView.config.entry.max_sessions_per_user");
  }

  public String getAdminConfigEntrySessionTimeout() {
    return getTranslation("adminView.config.entry.session_timeout_seconds");
  }

  public String getAdminConfigEntryMaintenanceMode() {
    return getTranslation("adminView.config.entry.maintenance_mode");
  }

  public String getAdminConfigEntryEmailFrom() {
    return getTranslation("adminView.config.entry.email_from");
  }

  public String getAdminConfigEntryEmailAdmin() {
    return getTranslation("adminView.config.entry.email_admin");
  }

  public String getAdminConfigEntryVerificationTokenValidDuration() {
    return getTranslation("adminView.config.entry.verification_token_valid_duration");
  }

  public String getAdminConfigEntryVerificationTokenLiveDuration() {
    return getTranslation("adminView.config.entry.verification_token_live_duration");
  }

  public String getAdminConfigEntryPasswordResetTokenValidDuration() {
    return getTranslation("adminView.config.entry.password_reset_token_valid_duration");
  }

  public String getAdminConfigEntryPasswordResetTokenLiveDuration() {
    return getTranslation("adminView.config.entry.password_reset_token_live_duration");
  }

  public String getAdminConfigEntryMailHost() {
    return getTranslation("adminView.config.entry.mail_host");
  }

  public String getAdminConfigEntryMailPort() {
    return getTranslation("adminView.config.entry.mail_port");
  }

  public String getAdminConfigEntryMailUsername() {
    return getTranslation("adminView.config.entry.mail_username");
  }

  public String getAdminConfigEntryMailPassword() {
    return getTranslation("adminView.config.entry.mail_password");
  }

  public String getAdminConfigEntryMailSmtpAuth() {
    return getTranslation("adminView.config.entry.mail_smtp_auth");
  }

  public String getAdminConfigEntryMailSmtpStarttlsEnable() {
    return getTranslation("adminView.config.entry.mail_smtp_starttls_enable");
  }

  public String getAdminConfigEntryMailSmtpSslEnable() {
    return getTranslation("adminView.config.entry.mail_smtp_ssl_enable");
  }

  public String getAdminConfigEntryMailSmtpSslTrust() {
    return getTranslation("adminView.config.entry.mail_smtp_ssl_trust");
  }

  public String getAdminConfigEntryMailDebug() {
    return getTranslation("adminView.config.entry.mail_debug");
  }

  public String getAdminConfigEntryEmailQueuePoolSize() {
    return getTranslation("adminView.config.entry.email_queue_pool_size");
  }

  public String getAdminConfigEntryEmailQueueCapacity() {
    return getTranslation("adminView.config.entry.email_queue_capacity");
  }

  public String getAdminConfigEntryEmailQueueMaxRetry() {
    return getTranslation("adminView.config.entry.email_queue_max_retry");
  }

  public String getAdminConfigEntryEmailQueueSentLiveDuration() {
    return getTranslation("adminView.config.entry.email_queue_sent_live_duration");
  }

  // Feldhilfen der Konfigurationseinträge
  public String getAdminConfigEntryServiceNameTooltip() {
    return getTranslation("adminView.config.entry.service_name.tooltip");
  }

  public String getAdminConfigEntryBaseUrlTooltip() {
    return getTranslation("adminView.config.entry.base_url.tooltip");
  }

  public String getAdminConfigEntryRememberMeDurationTooltip() {
    return getTranslation("adminView.config.entry.remember_me_duration.tooltip");
  }

  public String getAdminConfigEntryRememberMeSecretKeyTooltip() {
    return getTranslation("adminView.config.entry.remember_me_secret_key.tooltip");
  }

  public String getAdminConfigEntryUserAutoAbsentTimeoutTooltip() {
    return getTranslation("adminView.config.entry.user_auto_absent_timeout.tooltip");
  }

  public String getAdminConfigEntryLoginAttemptsLimitTooltip() {
    return getTranslation("adminView.config.entry.login_attempts_limit.tooltip");
  }

  public String getAdminConfigEntryMaxSessionsPerUserTooltip() {
    return getTranslation("adminView.config.entry.max_sessions_per_user.tooltip");
  }

  public String getAdminConfigEntrySessionTimeoutTooltip() {
    return getTranslation("adminView.config.entry.session_timeout_seconds.tooltip");
  }

  public String getAdminConfigEntryMaintenanceModeTooltip() {
    return getTranslation("adminView.config.entry.maintenance_mode.tooltip");
  }

  public String getAdminConfigEntryEmailFromTooltip() {
    return getTranslation("adminView.config.entry.email_from.tooltip");
  }

  public String getAdminConfigEntryEmailAdminTooltip() {
    return getTranslation("adminView.config.entry.email_admin.tooltip");
  }

  public String getAdminConfigEntryVerificationTokenValidDurationTooltip() {
    return getFormattedTranslation(
        "adminView.config.entry.verification_token_valid_duration.tooltip",
        TokenDurationPolicy.MINIMUM_TOKEN_VALIDITY.toMinutes());
  }

  public String getAdminConfigEntryVerificationTokenLiveDurationTooltip() {
    return getFormattedTranslation(
        "adminView.config.entry.verification_token_live_duration.tooltip",
        TokenDurationPolicy.MINIMUM_TOKEN_RETENTION.toMinutes());
  }

  public String getAdminConfigEntryPasswordResetTokenValidDurationTooltip() {
    return getFormattedTranslation(
        "adminView.config.entry.password_reset_token_valid_duration.tooltip",
        TokenDurationPolicy.MINIMUM_TOKEN_VALIDITY.toMinutes());
  }

  public String getAdminConfigEntryPasswordResetTokenLiveDurationTooltip() {
    return getFormattedTranslation(
        "adminView.config.entry.password_reset_token_live_duration.tooltip",
        TokenDurationPolicy.MINIMUM_TOKEN_RETENTION.toMinutes());
  }

  public String getAdminConfigEntryMailHostTooltip() {
    return getTranslation("adminView.config.entry.mail_host.tooltip");
  }

  public String getAdminConfigEntryMailPortTooltip() {
    return getTranslation("adminView.config.entry.mail_port.tooltip");
  }

  public String getAdminConfigEntryMailUsernameTooltip() {
    return getTranslation("adminView.config.entry.mail_username.tooltip");
  }

  public String getAdminConfigEntryMailPasswordTooltip() {
    return getTranslation("adminView.config.entry.mail_password.tooltip");
  }

  public String getAdminConfigEntryMailSmtpAuthTooltip() {
    return getTranslation("adminView.config.entry.mail_smtp_auth.tooltip");
  }

  public String getAdminConfigEntryMailSmtpStarttlsEnableTooltip() {
    return getTranslation("adminView.config.entry.mail_smtp_starttls_enable.tooltip");
  }

  public String getAdminConfigEntryMailSmtpSslEnableTooltip() {
    return getTranslation("adminView.config.entry.mail_smtp_ssl_enable.tooltip");
  }

  public String getAdminConfigEntryMailSmtpSslTrustTooltip() {
    return getTranslation("adminView.config.entry.mail_smtp_ssl_trust.tooltip");
  }

  public String getAdminConfigEntryMailDebugTooltip() {
    return getTranslation("adminView.config.entry.mail_debug.tooltip");
  }

  public String getAdminConfigEntryEmailQueuePoolSizeTooltip() {
    return getTranslation("adminView.config.entry.email_queue_pool_size.tooltip");
  }

  public String getAdminConfigEntryEmailQueueCapacityTooltip() {
    return getTranslation("adminView.config.entry.email_queue_capacity.tooltip");
  }

  public String getAdminConfigEntryEmailQueueMaxRetryTooltip() {
    return getTranslation("adminView.config.entry.email_queue_max_retry.tooltip");
  }

  public String getAdminConfigEntryEmailQueueSentLiveDurationTooltip() {
    return getTranslation("adminView.config.entry.email_queue_sent_live_duration.tooltip");
  }

  // Validierung der Konfiguration
  public String getAdminConfigValidationInvalidFields() {
    return getTranslation("adminView.config.validation.invalid_fields");
  }

  public String getAdminConfigValidationUrl() {
    return getTranslation("adminView.config.validation.url");
  }

  public String getAdminConfigValidationEmail() {
    return getTranslation("adminView.config.validation.email");
  }

  public String getAdminConfigValidationHost() {
    return getTranslation("adminView.config.validation.host");
  }

  public String getAdminConfigValidationPort() {
    return getTranslation("adminView.config.validation.port");
  }

  public String getAdminConfigValidationMinOne() {
    return getTranslation("adminView.config.validation.min_one");
  }

  public String getAdminConfigValidationNonNegative() {
    return getTranslation("adminView.config.validation.non_negative");
  }

  public String getAdminConfigValidationSecretKey(int minimumKeyLength) {
    return getFormattedTranslation("adminView.config.validation.secret_key", minimumKeyLength);
  }

  public String getAdminConfigValidationDurationMinimum(long minimumMinutes) {
    return getFormattedTranslation("adminView.config.validation.duration_minimum", minimumMinutes);
  }

  public String getAdminConfigValidationTokenDuration(long minimumTokenMinutes) {
    return getFormattedTranslation(
        "adminView.config.validation.token_duration", minimumTokenMinutes);
  }

  public String getAdminConfigValidationPositiveDuration() {
    return getTranslation("adminView.config.validation.positive_duration");
  }

  public String getAdminConfigValidationDuration() {
    return getTranslation("adminView.config.validation.duration");
  }

  public String getAdminConfigValidationServiceName() {
    return getTranslation("adminView.config.validation.service_name");
  }

  public String getAdminConfigValidationServiceNameMax(int maximumServiceNameLength) {
    return getFormattedTranslation(
        "adminView.config.validation.service_name_max", maximumServiceNameLength);
  }

  public String getAdminConfigHelperServiceNameMax(int maximumServiceNameLength) {
    return getFormattedTranslation(
        "adminView.config.helper.service_name_max", maximumServiceNameLength);
  }

  // Benutzerverwaltung
  public String getAdminUsersTitle() {
    return getTranslation("admin.users.title");
  }

  public String getAdminUsersCardTitle() {
    return getTranslation("admin.users.card_title");
  }

  public String getAdminUsersCardDescription() {
    return getTranslation("admin.users.card_description");
  }

  public String getAdminUsersEmptyHint() {
    return getTranslation("admin.users.empty_hint");
  }

  public String getAdminUsersAccountsLabel() {
    return getTranslation("admin.users.accounts_label");
  }

  public String getAdminUsersSelf() {
    return getTranslation("admin.users.self");
  }

  public String getAdminUsersAccountCount(int accountCount) {
    return getFormattedTranslation(
        accountCount == 1 ? "admin.users.account_count.one" : "admin.users.account_count.other",
        accountCount);
  }

  public String getAdminUsersDescription() {
    return getTranslation("admin.users.description");
  }

  public String getAdminUsersAdd() {
    return getTranslation("admin.users.add");
  }

  public String getAdminUsersEdit() {
    return getTranslation("admin.users.edit");
  }

  public String getAdminUsersDelete() {
    return getTranslation("admin.users.delete");
  }

  public String getAdminUsersSave() {
    return getTranslation("admin.users.save");
  }

  public String getAdminUsersCancel() {
    return getTranslation("admin.users.cancel");
  }

  public String getAdminUsersYes() {
    return getTranslation("admin.users.yes");
  }

  public String getAdminUsersNo() {
    return getTranslation("admin.users.no");
  }

  public String getAdminUsersGridEmail() {
    return getTranslation("admin.users.grid.email");
  }

  public String getAdminUsersGridEnabled() {
    return getTranslation("admin.users.grid.enabled");
  }

  public String getAdminUsersGridRoles() {
    return getTranslation("admin.users.grid.roles");
  }

  public String getAdminUsersGridActions() {
    return getTranslation("admin.users.grid.actions");
  }

  public String getAdminUsersFieldFirstName() {
    return getTranslation("admin.users.field.first_name");
  }

  public String getAdminUsersFieldLastName() {
    return getTranslation("admin.users.field.last_name");
  }

  public String getAdminUsersFieldEmail() {
    return getTranslation("admin.users.field.email");
  }

  public String getAdminUsersFieldPassword() {
    return getTranslation("admin.users.field.password");
  }

  public String getAdminUsersFieldEnabled() {
    return getTranslation("admin.users.field.enabled");
  }

  public String getAdminUsersEnabledHint() {
    return getTranslation("admin.users.field.enabled_hint");
  }

  public String getAdminUsersFieldRoles() {
    return getTranslation("admin.users.field.roles");
  }

  public String getAdminUsersCreateDialogTitle() {
    return getTranslation("admin.users.dialog.create_title");
  }

  public String getAdminUsersEditDialogTitle() {
    return getTranslation("admin.users.dialog.edit_title");
  }

  public String getAdminUsersDialogDescription() {
    return getTranslation("admin.users.dialog.description");
  }

  public String getAdminUsersCloseDialog() {
    return getTranslation("admin.users.dialog.close");
  }

  public String getAdminUsersPasswordPlaceholderEdit() {
    return getTranslation("admin.users.password.placeholder.edit");
  }

  public String getAdminUsersDeleteSuccess() {
    return getTranslation("admin.users.delete_success");
  }

  public String getAdminUsersSaveSuccessCreated() {
    return getTranslation("admin.users.save_success_created");
  }

  public String getAdminUsersSaveSuccessUpdated() {
    return getTranslation("admin.users.save_success_updated");
  }

  // Geschlecht und Rollen
  public String getGenderMale() {
    return getTranslation("gender.male");
  }

  public String getGenderFemale() {
    return getTranslation("gender.female");
  }

  public String getGenderOther() {
    return getTranslation("gender.other");
  }

  public String getRoleSuperadmin() {
    return getTranslation("role.superadmin");
  }

  public String getRoleGuest() {
    return getTranslation("role.guest");
  }

  public String getRoleAdmin() {
    return getTranslation("role.admin");
  }

  public String getRoleUser() {
    return getTranslation("role.user");
  }

  // Benutzertabelle, Suche und Filter
  public String getAdminUsersGridName() {
    return getTranslation("admin.users.grid.name");
  }

  public String getAdminUsersGridStatus() {
    return getTranslation("admin.users.grid.status");
  }

  public String getAdminUsersGridLastActivity() {
    return getTranslation("admin.users.grid.last_activity");
  }

  public String getAdminUsersGridEmptyState() {
    return getTranslation("admin.users.grid.empty_state");
  }

  public String getAdminUsersSearchLabel() {
    return getTranslation("admin.users.search.label");
  }

  public String getAdminUsersSearchPlaceholder() {
    return getTranslation("admin.users.search.placeholder");
  }

  public String getAdminUsersClearFilters() {
    return getTranslation("admin.users.filter.clear");
  }

  public String getAdminUsersApplyFilters() {
    return getTranslation("admin.users.filter.apply");
  }

  public String getAdminUsersActiveFilterCount(int activeFilterCount) {
    return getFormattedTranslation("admin.users.filter.active_count", activeFilterCount);
  }

  public String getAdminUsersFilterTimeHint() {
    return getTranslation("admin.users.filter.time_hint");
  }

  public String getAdminUsersAddFilter() {
    return getTranslation("admin.users.filter.add");
  }

  public String getAdminUsersFilterPanelTitle() {
    return getTranslation("admin.users.filter.title");
  }

  public String getAdminUsersFilterPanelDescription() {
    return getTranslation("admin.users.filter.description");
  }

  public String getAdminUsersFilterAll() {
    return getTranslation("admin.users.filter.all");
  }

  public String getAdminUsersFilterRole() {
    return getTranslation("admin.users.filter.role");
  }

  public String getAdminUsersActivityDate() {
    return getTranslation("admin.users.activity.date");
  }

  public String getAdminUsersActivityTimeOptional() {
    return getTranslation("admin.users.activity.time_optional");
  }

  // Zeitangaben zur Benutzeraktivität
  public String getAdminUsersActivityNow() {
    return getTranslation("admin.users.activity.now");
  }

  public String getAdminUsersActivityMinutes(long elapsedMinutes) {
    return getFormattedTranslation(
        elapsedMinutes == 1
            ? "admin.users.activity.minutes.one"
            : "admin.users.activity.minutes.many",
        elapsedMinutes);
  }

  public String getAdminUsersActivityHours(long elapsedHours) {
    return getFormattedTranslation(
        elapsedHours == 1 ? "admin.users.activity.hours.one" : "admin.users.activity.hours.many",
        elapsedHours);
  }

  public String getAdminUsersActivityDays(long elapsedDays) {
    return getFormattedTranslation(
        elapsedDays == 1 ? "admin.users.activity.days.one" : "admin.users.activity.days.many",
        elapsedDays);
  }

  public String getAdminUsersActivityWeeks(long elapsedWeeks) {
    return getFormattedTranslation(
        elapsedWeeks == 1 ? "admin.users.activity.weeks.one" : "admin.users.activity.weeks.many",
        elapsedWeeks);
  }

  public String getAdminUsersActivityMonths(long elapsedMonths) {
    return getFormattedTranslation(
        elapsedMonths == 1 ? "admin.users.activity.months.one" : "admin.users.activity.months.many",
        elapsedMonths);
  }

  public String getAdminUsersActivityYears(long elapsedYears) {
    return getFormattedTranslation(
        elapsedYears == 1 ? "admin.users.activity.years.one" : "admin.users.activity.years.many",
        elapsedYears);
  }

  public String getAdminUsersFilterActiveSince() {
    return getTranslation("admin.users.filter.active_since");
  }

  public String getAdminUsersFilterActiveSinceHelp() {
    return getTranslation("admin.users.filter.active_since_help");
  }

  public String getAdminUsersFilterActiveSinceError() {
    return getTranslation("admin.users.filter.active_since_error");
  }

  public String getAdminUsersPaginationPage(int pageNumber) {
    return getFormattedTranslation("admin.users.pagination.page", pageNumber);
  }

  public String getAdminUsersRefreshTooltip() {
    return getTranslation("admin.users.refresh.tooltip");
  }

  public String getAdminUsersRefreshSuccess() {
    return getTranslation("admin.users.refresh.success");
  }

  public String getAdminUsersRefreshFailed() {
    return getTranslation("admin.users.refresh.failed");
  }

  public String getAdminUsersPaginationPerPage() {
    return getTranslation("admin.users.pagination.per_page");
  }

  public String getAdminUsersPaginationFirstPage() {
    return getTranslation("admin.users.pagination.first_page");
  }

  public String getAdminUsersPaginationPreviousPage() {
    return getTranslation("admin.users.pagination.previous_page");
  }

  public String getAdminUsersPaginationNextPage() {
    return getTranslation("admin.users.pagination.next_page");
  }

  public String getAdminUsersPaginationLastPage() {
    return getTranslation("admin.users.pagination.last_page");
  }

  public String getAdminUsersDeleteDialogTitle() {
    return getTranslation("admin.users.delete_dialog.title");
  }

  public String getAdminUsersDeleteDialogDescription() {
    return getTranslation("admin.users.delete_dialog.description");
  }

  public String getAdminUsersDeleteDialogQuestion() {
    return getTranslation("admin.users.delete_dialog.question");
  }

  public String getAdminUsersDeleteDialogWarning() {
    return getTranslation("admin.users.delete_dialog.warning");
  }

  public String getAdminUsersPasswordHelperEdit() {
    return getFormattedTranslation(
        "admin.users.password.helper.edit", getAdminUsersPasswordHelperCreate());
  }

  public String getAdminUsersPasswordHelperCreate() {
    return getTranslation("admin.users.password.helper.create");
  }

  public String getAdminUsersPasswordConfirmHelper() {
    return getTranslation("admin.users.password.confirm.helper");
  }

  public String getAdminUsersPasswordValidationEdit() {
    return getFormattedTranslation(
        "admin.users.password.validation.edit", getBaseValidationPasswordInvalidMessage());
  }

  public String getAdminUsersPasswordValidationCreate() {
    return getFormattedTranslation(
        "admin.users.password.validation.create", getBaseValidationPasswordInvalidMessage());
  }

  public String getAdminUsersSelectedRoles(int selectedRoleCount) {
    return getFormattedTranslation(
        selectedRoleCount == 1
            ? "admin.users.roles.selected.one"
            : "admin.users.roles.selected.many",
        selectedRoleCount);
  }

  public String getAdminUsersRolesHelper() {
    return getTranslation("admin.users.roles.helper");
  }

  public String getAdminUsersTechnicalInfoEdit() {
    return getTranslation("admin.users.technical_info.edit");
  }

  public String getAdminUsersTechnicalInfoCreate() {
    return getTranslation("admin.users.technical_info.create");
  }

  public String getAdminUsersValidationCheckFields() {
    return getTranslation("admin.users.validation.check_fields");
  }

  public String getAdminUsersErrorRoleMissing() {
    return getTranslation("admin.users.error.role_missing");
  }

  public String getAdminUsersErrorDisableOwnUser() {
    return getTranslation("admin.users.error.disable_own_user");
  }

  public String getAdminUsersErrorRemoveOwnAdminRole() {
    return getTranslation("admin.users.error.remove_own_admin_role");
  }

  public String getAdminUsersErrorLastActiveSuperadmin() {
    return getTranslation("admin.users.error.last_active_superadmin");
  }

  public String getAdminUsersErrorSaveFailed() {
    return getTranslation("admin.users.error.save_failed");
  }

  public String getAdminUsersErrorDeleteOwnUser() {
    return getTranslation("admin.users.error.delete_own_user");
  }

  public String getAdminUsersErrorDeleteLastSuperadmin() {
    return getTranslation("admin.users.error.delete_last_superadmin");
  }

  public String getAdminUsersErrorDeleteFailed() {
    return getTranslation("admin.users.error.delete_failed");
  }

  /** Group identity is independent of the properties key and the current label. */
  public String getAdminConfigGroupLabel(AdminConfigGroup configGroup) {
    return switch (configGroup) {
      case GENERAL -> getAdminConfigGroupGeneral();
      case SECURITY -> getAdminConfigGroupSecurity();
      case EXTERNAL_LOGIN -> getProviderSettingsTitle();
      case TWO_FACTOR -> getTwoFactorSettingsTitle();
      case TOKENS -> getAdminConfigGroupTokens();
      case USER_STATUS -> getAdminConfigGroupUserStatus();
      case EMAIL_SENDER -> getAdminConfigGroupEmailSender();
      case SMTP -> getAdminConfigGroupSmtp();
      case NOTIFICATIONS -> getAdminConfigGroupNotifications();
      case QUEUE -> getAdminConfigGroupQueue();
    };
  }

  /** Sprachabhängiger Standard-Fußtext; die benannten Platzhalter ersetzt die Kontoansicht. */
  public String getAccountCopyrightTemplate() {
    return getTranslation("account.copyright.template");
  }

  // Wartungsmodus
  public String getMaintenanceTitle() {
    return getTranslation("maintenance.title");
  }

  public String getMaintenanceDescription() {
    return getTranslation("maintenance.description");
  }

  public String getMaintenanceLoginButton() {
    return getTranslation("maintenance.login.button");
  }

  public String getMaintenanceRetryButton() {
    return getTranslation("maintenance.retry.button");
  }

  public String getMaintenanceStatusAvailable() {
    return getTranslation("maintenance.status.available");
  }

  public String getMaintenanceStatusActive() {
    return getTranslation("maintenance.status.active");
  }

  public String getMaintenanceStatusUnavailable() {
    return getTranslation("maintenance.status.unavailable");
  }

  public String getMaintenanceLoginMessage() {
    return getTranslation("maintenance.login.message");
  }

  public String getMaintenanceUnavailableMessage() {
    return getTranslation("maintenance.unavailable.message");
  }

  // Kontonavigation und Kontoeinstellungen
  public String getAccountNavigationPortal() {
    return getTranslation("account.navigation.portal");
  }

  public String getAccountOpenNavigation() {
    return getTranslation("account.navigation.open");
  }

  public String getAccountOpenUserMenu(String accountDisplayName) {
    return getFormattedTranslation("account.user.menu.open", accountDisplayName);
  }

  public String getAccountEmailAddressLabel() {
    return getTranslation("account.profile.email.label");
  }

  public String getAccountReadOnlyLabel() {
    return getTranslation("account.profile.readonly");
  }

  public String getAccountBirthDateFormatHint() {
    return getTranslation("account.profile.date.format");
  }

  public String getAccountSettingsOverview() {
    return getTranslation("accountSettings.overview");
  }

  public String getAccountSettingsTitle() {
    return getTranslation("accountSettings.title");
  }

  public String getAccountSettingsDescription() {
    return getTranslation("accountSettings.description");
  }

  public String getAccountSettingsProfileTab() {
    return getTranslation("accountSettings.profile.tab");
  }

  public String getAccountSettingsSecurityTab() {
    return getTranslation("accountSettings.security.tab");
  }

  public String getAccountSettingsSessionsTab() {
    return getTranslation("accountSettings.sessions.tab");
  }

  public String getAccountSettingsEditPicture() {
    return getTranslation("accountSettings.edit.picture");
  }

  // Profil und Sprache
  public String getAccountSettingsProfileTitle() {
    return getTranslation("accountSettings.profile.title");
  }

  public String getAccountSettingsProfileDescription() {
    return getTranslation("accountSettings.profile.description");
  }

  public String getAccountSettingsLanguage() {
    return getTranslation("accountSettings.language");
  }

  public String getAccountSettingsLanguageHint() {
    return getTranslation("accountSettings.language.hint");
  }

  public String getAccountSettingsChangeEmail() {
    return getTranslation("accountSettings.change.email");
  }

  public String getAccountSettingsChangePassword() {
    return getTranslation("accountSettings.change.password");
  }

  public String getAccountSettingsPasswordHint() {
    return getTranslation("accountSettings.password.hint");
  }

  public String getAccountPasswordConfigured() {
    return getTranslation("accountSettings.password.configured");
  }

  public String getAccountPasswordNotConfigured() {
    return getTranslation("accountSettings.password.notConfigured");
  }

  public String getAccountPasswordSetupAction() {
    return getTranslation("accountSettings.password.setupAction");
  }

  public String getAccountSettingsAccountHint() {
    return getTranslation("accountSettings.account.hint");
  }

  public String getAccountSettingsSave() {
    return getTranslation("accountSettings.save");
  }

  // Kontosicherheit und Sitzungen
  public String getAccountSettingsSecurityTitle() {
    return getTranslation("accountSettings.security.title");
  }

  public String getAccountSettingsChangeAction() {
    return getTranslation("accountSettings.change.action");
  }

  public String getAccountSettingsSecurityIntroduction() {
    return getTranslation("accountSettings.security.introduction");
  }

  public String getAccountSettingsSessionsIntroduction() {
    return getTranslation("accountSettings.sessions.introduction");
  }

  public String getAccountSettingsSecurityDescription() {
    return getTranslation("accountSettings.security.description");
  }

  public String getAccountSettingsSessionsTitle() {
    return getTranslation("accountSettings.sessions.title");
  }

  public String getAccountSettingsSessionsDescription() {
    return getTranslation("accountSettings.sessions.description");
  }

  public String getAccountSettingsSessionsMeta() {
    return getTranslation("accountSettings.sessions.meta");
  }

  public String getAccountSettingsSessionsActivity() {
    return getTranslation("accountSettings.sessions.activity");
  }

  public String getAccountSettingsSessionsLimit(int activeSessionLimit) {
    return getFormattedTranslation("accountSettings.sessions.limit", activeSessionLimit);
  }

  public String getAccountSettingsCurrentDevice() {
    return getTranslation("accountSettings.current.device");
  }

  public String getAccountSettingsCurrentSession() {
    return getTranslation("accountSettings.current.session");
  }

  public String getAccountSettingsLogoutOthers() {
    return getTranslation("accountSettings.logout.others");
  }

  public String getAccountSettingsSaveSuccess() {
    return getTranslation("accountSettings.save.success");
  }

  public String getAccountSettingsSaveFailed() {
    return getTranslation("accountSettings.save.failed");
  }

  // E-Mail-Adresse und Passwort ändern
  public String getAccountEmailDialogDescription() {
    return getTranslation("accountSettings.email.dialog.description");
  }

  public String getAccountEmailDialogNewEmail() {
    return getTranslation("accountSettings.email.dialog.new.email");
  }

  public String getAccountEmailDialogConfirmEmail() {
    return getTranslation("accountSettings.email.dialog.confirm.email");
  }

  public String getAccountEmailDialogPassword() {
    return getTranslation("accountSettings.email.dialog.password");
  }

  public String getAccountEmailDialogHint() {
    return getTranslation("accountSettings.email.dialog.hint");
  }

  public String getAccountEmailDialogRequest() {
    return getTranslation("accountSettings.email.dialog.request");
  }

  public String getAccountPasswordDialogDescription() {
    return getTranslation("accountSettings.password.dialog.description");
  }

  public String getAccountPasswordDialogCurrentPassword() {
    return getTranslation("accountSettings.password.dialog.current.password");
  }

  public String getAccountPasswordDialogHint() {
    return getTranslation("accountSettings.password.dialog.hint");
  }

  public String getAccountPasswordDialogConfirmPassword() {
    return getTranslation("accountSettings.password.dialog.confirm.password");
  }

  public String getAccountPasswordChangedSubject() {
    return getTranslation("account.security.password.subject");
  }

  public String getAccountEmailChangeSubject() {
    return getTranslation("account.security.email.request.subject");
  }

  public String getAccountEmailChangedSubject() {
    return getTranslation("account.security.email.changed.subject");
  }

  public String getAccountSecurityPasswordSuccess() {
    return getTranslation("account.security.password.success");
  }

  public String getAccountSecurityEmailRequested() {
    return getTranslation("account.security.email.requested");
  }

  public String getAccountSecurityWrongPassword() {
    return getTranslation("account.security.password.wrong");
  }

  public String getAccountSecurityFailed() {
    return getTranslation("account.security.failed");
  }

  public String getAccountSecuritySameEmail() {
    return getTranslation("account.security.email.same");
  }

  public String getAccountSecuritySamePassword() {
    return getTranslation("account.security.password.same");
  }

  public String getAccountSecurityRequestPending() {
    return getTranslation("account.security.email.pending");
  }

  // Änderung der E-Mail-Adresse bestätigen
  public String getAccountEmailConfirmationTitle() {
    return getTranslation("account.security.email.confirm.title");
  }

  public String getAccountEmailConfirmationReady() {
    return getTranslation("account.security.email.confirm.ready");
  }

  public String getAccountEmailConfirmationSuccess() {
    return getTranslation("account.security.email.confirm.success");
  }

  public String getAccountEmailConfirmationInvalid() {
    return getTranslation("account.security.email.confirm.invalid");
  }

  public String getAccountEmailConfirmationExpired() {
    return getTranslation("account.security.email.confirm.expired");
  }

  public String getAccountEmailConfirmationConflict() {
    return getTranslation("account.security.email.confirm.conflict");
  }

  public String getAccountEmailConfirmationFailed() {
    return getTranslation("account.security.email.confirm.failed");
  }

  public String getAccountEmailConfirmationAction() {
    return getTranslation("account.security.email.confirm.action");
  }

  // Sitzungsverwaltung und Anmeldung
  public String getSessionManagementHint() {
    return getTranslation("sessions.management.hint");
  }

  public String getSessionRefresh() {
    return getTranslation("sessions.refresh");
  }

  public String getSessionUnknownDevice() {
    return getTranslation("sessions.unknown.device");
  }

  public String getSessionSignedInVia(String signInMethodName) {
    return getSessionSignedInViaLabel() + ": " + signInMethodName;
  }

  public String getSessionSignedInViaLabel() {
    return getTranslation("signIn.sessionVia");
  }

  public String getSignInDetailsTitle() {
    return getTranslation("signIn.detailsTitle");
  }

  public String getSignInRegisteredVia() {
    return getTranslation("signIn.registeredVia");
  }

  public String getSignInAvailableMethods() {
    return getTranslation("signIn.availableMethods");
  }

  public String getSignInPortal() {
    return getTranslation("signIn.portal");
  }

  public String getSignInPortalPassword() {
    return getTranslation("signIn.portalPassword");
  }

  public String getSignInAccountCreated() {
    return getTranslation("signIn.accountCreated");
  }

  public String getSignInSavedLogin() {
    return getTranslation("signIn.savedLogin");
  }

  public String getSignInUnknown() {
    return getTranslation("signIn.unknown");
  }

  public String getSignInNone() {
    return getTranslation("signIn.none");
  }

  public String getSessionLastActivity() {
    return getTranslation("sessions.last.activity");
  }

  public String getSessionStarted() {
    return getTranslation("sessions.started");
  }

  public String getSessionRevoked() {
    return getTranslation("sessions.revoked");
  }

  public String getSessionManagementFailed() {
    return getTranslation("sessions.management.failed");
  }

  public String getSessionRecoveryHint() {
    return getTranslation("sessions.recovery.hint");
  }

  public String getSessionRefreshSuccess() {
    return getTranslation("sessions.refresh.success");
  }

  public String getSessionRecoveryTitle() {
    return getTranslation("sessions.recovery.title");
  }

  public String getSessionRecoveryListHint() {
    return getTranslation("sessions.recovery.list.hint");
  }

  public String getSessionRecoveryLogoutAll() {
    return getTranslation("sessions.recovery.logout.all");
  }

  public String getSessionContinueLogin() {
    return getTranslation("sessions.continue.login");
  }

  public String getSessionRecoveryExpired() {
    return getTranslation("sessions.recovery.expired");
  }

  public String getSessionRecoveryLogoutHint() {
    return getTranslation("sessions.recovery.logout.hint");
  }

  public String getSessionSavedSince() {
    return getTranslation("sessions.saved.since");
  }

  // Profilbild
  public String getAccountAvatarDescription() {
    return getTranslation("account.avatar.description");
  }

  public String getAccountAvatarMode() {
    return getTranslation("account.avatar.mode");
  }

  public String getAccountAvatarInitials() {
    return getTranslation("account.avatar.initials");
  }

  public String getAccountAvatarCustom() {
    return getTranslation("account.avatar.custom");
  }

  public String getAccountAvatarChoose() {
    return getTranslation("account.avatar.choose");
  }

  public String getAccountAvatarUploadHint() {
    return getTranslation("account.avatar.upload.hint");
  }

  public String getAccountAvatarCropHint() {
    return getTranslation("account.avatar.crop.hint");
  }

  public String getAccountAvatarZoom() {
    return getTranslation("account.avatar.zoom");
  }

  public String getAccountAvatarResetCrop() {
    return getTranslation("account.avatar.reset");
  }

  public String getAccountAvatarKeepHint() {
    return getTranslation("account.avatar.keep.hint");
  }

  public String getAccountAvatarDelete() {
    return getTranslation("account.avatar.delete");
  }

  public String getAccountAvatarDeleteQuestion() {
    return getTranslation("account.avatar.delete.question");
  }

  public String getAccountAvatarDeleteExplanation() {
    return getTranslation("account.avatar.delete.explanation");
  }

  public String getAccountAvatarDeleteHint() {
    return getTranslation("account.avatar.delete.hint");
  }

  public String getAccountAvatarTypeError() {
    return getTranslation("account.avatar.error.type");
  }

  public String getAccountAvatarSizeError() {
    return getTranslation("account.avatar.error.size");
  }

  public String getAccountAvatarImageError() {
    return getTranslation("account.avatar.error.image");
  }

  public String getAccountAvatarDimensionsError() {
    return getTranslation("account.avatar.error.dimensions");
  }

  public String getAccountAvatarSaved() {
    return getTranslation("account.avatar.saved");
  }

  public String getAccountAvatarSaveFailed() {
    return getTranslation("account.avatar.save.failed");
  }

  public String getAccountAvatarConflict() {
    return getTranslation("account.avatar.conflict");
  }

  public String getAccountAvatarCropError() {
    return getTranslation("account.avatar.error.crop");
  }

  public String getAccountAvatarBusy() {
    return getTranslation("account.avatar.busy");
  }

  public String getAccountAvatarExpired() {
    return getTranslation("account.avatar.expired");
  }

  public String getAccountAvatarMissing() {
    return getTranslation("account.avatar.missing");
  }

  public String getAccountAvatarOutputError() {
    return getTranslation("account.avatar.error.output");
  }

  // Persönliche Mitteilungen
  public String getNotificationsTitle() {
    return getTranslation("notifications.title");
  }

  public String getNotificationsInboxDescription() {
    return getTranslation("notifications.inboxDescription");
  }

  public String getNotificationsTotal() {
    return getTranslation("notifications.total");
  }

  public String getNotificationsSearch() {
    return getTranslation("notifications.search");
  }

  public String getNotificationsSearchEmpty() {
    return getTranslation("notifications.searchEmpty");
  }

  public String getNotificationsPageDescription() {
    return getTranslation("notifications.pageDescription");
  }

  public String getNotificationsDetailTitle() {
    return getTranslation("notifications.detailTitle");
  }

  public String getNotificationsDetailDescription() {
    return getTranslation("notifications.detailDescription");
  }

  public String getNotificationsDescription() {
    return getTranslation("notifications.description");
  }

  public String getNotificationsLatest(int notificationLimit) {
    return getFormattedTranslation("notifications.latest", notificationLimit);
  }

  public String getNotificationsLatestUnread(int unreadNotificationLimit) {
    return getFormattedTranslation("notifications.latestUnread", unreadNotificationLimit);
  }

  public String getNotificationsLatestRead(int readNotificationLimit) {
    return getFormattedTranslation("notifications.latestRead", readNotificationLimit);
  }

  public String getNotificationsAll() {
    return getTranslation("notifications.all");
  }

  public String getNotificationsFilter() {
    return getTranslation("notifications.filter");
  }

  public String getNotificationsDismiss() {
    return getTranslation("notifications.dismiss");
  }

  public String getNotificationsDismissRead() {
    return getTranslation("notifications.dismissRead");
  }

  public String getNotificationsOpen() {
    return getTranslation("notifications.open");
  }

  public String getNotificationsMarkRead() {
    return getTranslation("notifications.markRead");
  }

  public String getNotificationsSourceAdministration() {
    return getTranslation("notifications.sourceAdministration");
  }

  public String getNotificationsSourceSystem() {
    return getTranslation("notifications.sourceSystem");
  }

  public String getNotificationsUnread() {
    return getTranslation("notifications.unread");
  }

  public String getNotificationsMarkAll() {
    return getTranslation("notifications.markAll");
  }

  public String getNotificationsEmpty() {
    return getTranslation("notifications.empty");
  }

  public String getNotificationsHistory() {
    return getTranslation("notifications.history");
  }

  public String getNotificationsRefresh() {
    return getTranslation("notifications.refresh");
  }

  public String getNotificationsRefreshed() {
    return getTranslation("notifications.refreshed");
  }

  public String getNotificationsPrevious() {
    return getTranslation("notifications.previous");
  }

  public String getNotificationsNext() {
    return getTranslation("notifications.next");
  }

  public String getNotificationsRead() {
    return getTranslation("notifications.read");
  }

  public String getNotificationsClose() {
    return getTranslation("notifications.close");
  }

  public String getNotificationsUnavailable() {
    return getTranslation("notifications.unavailable");
  }

  public String getNotificationsOpenAccount() {
    return getTranslation("notifications.openAccount");
  }

  public String getNotificationsMoreActions() {
    return getTranslation("notifications.moreActions");
  }

  public String getNotificationsOpenTicket() {
    return getTranslation("notifications.openTicket");
  }

  public String getSupportTicketUnavailable() {
    return getTranslation("support.ticketUnavailable");
  }

  public String getAdminConfigUnlimited() {
    return getTranslation("adminView.config.unlimited");
  }

  public String getAdminConfigInitialValue() {
    return getTranslation("adminView.config.initial_value");
  }

  public String getAdminConfigRuleEmptyUnlimited() {
    return getTranslation("adminView.config.rule.empty_unlimited");
  }

  public String getNotificationsRetentionUnlimited() {
    return getTranslation("notifications.retentionUnlimited");
  }

  public String getNotificationsRetention(int retentionDays) {
    return getFormattedTranslation("notifications.retention", retentionDays);
  }

  public String getNotificationsPasswordChangedTitle() {
    return getTranslation("notifications.passwordChangedTitle");
  }

  public String getNotificationsPasswordChangedBody() {
    return getTranslation("notifications.passwordChangedBody");
  }

  public String getNotificationsPasswordResetTitle() {
    return getTranslation("notifications.passwordResetTitle");
  }

  public String getNotificationsPasswordResetBody() {
    return getTranslation("notifications.passwordResetBody");
  }

  public String getNotificationsEmailChangedTitle() {
    return getTranslation("notifications.emailChangedTitle");
  }

  public String getNotificationsEmailChangedBody() {
    return getTranslation("notifications.emailChangedBody");
  }

  // Versand und Versandverlauf von Mitteilungen
  public String getNotificationsAdminDescription() {
    return getTranslation("notifications.adminDescription");
  }

  public String getNotificationsComposeTitle() {
    return getTranslation("notifications.composeTitle");
  }

  public String getNotificationsInternal() {
    return getTranslation("notifications.internal");
  }

  public String getNotificationsDeliveryPaused() {
    return getTranslation("notifications.deliveryPaused");
  }

  public String getNotificationsDeliveryInfo() {
    return getTranslation("notifications.deliveryInfo");
  }

  public String getNotificationsEveryoneHint() {
    return getTranslation("notifications.everyoneHint");
  }

  public String getNotificationsSelectRecipients() {
    return getTranslation("notifications.selectRecipients");
  }

  public String getNotificationsReviewedPreview() {
    return getTranslation("notifications.reviewedPreview");
  }

  public String getNotificationsRecipientDetails() {
    return getTranslation("notifications.recipientDetails");
  }

  public String getNotificationsPreviewTitle() {
    return getTranslation("notifications.previewTitle");
  }

  public String getNotificationsReviewAgain() {
    return getTranslation("notifications.reviewAgain");
  }

  public String getNotificationsRecheckHint() {
    return getTranslation("notifications.recheckHint");
  }

  public String getNotificationsBeforeSendHint() {
    return getTranslation("notifications.beforeSendHint");
  }

  public String getNotificationsHistoryDetails() {
    return getTranslation("notifications.historyDetails");
  }

  public String getNotificationsMessageContent() {
    return getTranslation("notifications.messageContent");
  }

  public String getNotificationsHistoryDelivery() {
    return getTranslation("notifications.historyDelivery");
  }

  public String getNotificationsHistoryImmutableLog() {
    return getTranslation("notifications.historyImmutableLog");
  }

  public String getNotificationsContentPreview() {
    return getTranslation("notifications.contentPreview");
  }

  public String getNotificationsPreviewUpdated() {
    return getTranslation("notifications.previewUpdated");
  }

  public String getNotificationsEveryonePlaceholder(int eligibleUserCount) {
    return getFormattedTranslation("notifications.everyonePlaceholder", eligibleUserCount);
  }

  public String getNotificationsSelectedUsers(int selectedUserCount) {
    return getFormattedTranslation("notifications.selectedUsers", selectedUserCount);
  }

  public String getNotificationsPreviewNeedsReview() {
    return getTranslation("notifications.previewNeedsReview");
  }

  public String getNotificationsAdminPageDescription() {
    return getTranslation("notifications.adminPageDescription");
  }

  public String getNotificationsEveryone() {
    return getTranslation("notifications.everyone");
  }

  public String getNotificationsRecipients() {
    return getTranslation("notifications.recipients");
  }

  public String getNotificationsSubject() {
    return getTranslation("notifications.subject");
  }

  public String getNotificationsBody() {
    return getTranslation("notifications.body");
  }

  public String getNotificationsPreview() {
    return getTranslation("notifications.preview");
  }

  public String getNotificationsSend() {
    return getTranslation("notifications.send");
  }

  public String getNotificationsSent() {
    return getTranslation("notifications.sent");
  }

  public String getNotificationsHistoryDescription() {
    return getTranslation("notifications.historyDescription");
  }

  public String getNotificationsHistoryCount(long sentNotificationCount) {
    return getFormattedTranslation(
        sentNotificationCount == 1
            ? "notifications.historyCountSingle"
            : "notifications.historyCount",
        sentNotificationCount);
  }

  public String getNotificationsHistoryRecipients(int recipientCount) {
    return getFormattedTranslation(
        recipientCount == 1 ? "notifications.historyRecipient" : "notifications.historyRecipients",
        recipientCount);
  }

  public String getNotificationsSentStatus() {
    return getTranslation("notifications.sentStatus");
  }

  public String getNotificationsSentHistory() {
    return getTranslation("notifications.sentHistory");
  }

  public String getNotificationsSentEmpty() {
    return getTranslation("notifications.sentEmpty");
  }

  public String getNotificationsInvalidSubject() {
    return getTranslation("notifications.invalidSubject");
  }

  public String getNotificationsInvalidBody() {
    return getTranslation("notifications.invalidBody");
  }

  public String getNotificationsPreviewFailedTitle() {
    return getTranslation("notifications.previewFailedTitle");
  }

  public String getNotificationsPreviewFailedMessage() {
    return getTranslation("notifications.previewFailedMessage");
  }

  public String getNotificationsInvalidDraft() {
    return getTranslation("notifications.invalidDraft");
  }

  public String getNotificationsNoRecipients() {
    return getTranslation("notifications.noRecipients");
  }

  public String getNotificationsRecipientsChanged() {
    return getTranslation("notifications.recipientsChanged");
  }

  public String getNotificationsRequestConflict() {
    return getTranslation("notifications.requestConflict");
  }

  public String getNotificationsRolesChangedTitle() {
    return getTranslation("notifications.rolesChangedTitle");
  }

  public String getNotificationsRolesChangedBody() {
    return getTranslation("notifications.rolesChangedBody");
  }

  public String getRolesPermissionDenied() {
    return getTranslation("roles.permissionDenied");
  }

  // Mitteilungen im Portal löschen und als gelesen markieren
  public String getPortalDeleteAction() {
    return getTranslation("portal.action.delete");
  }

  public String getPortalNotificationSelectionHint() {
    return getTranslation("portal.notification.selection.hint");
  }

  public String getPortalDeleteNotificationQuestion() {
    return getTranslation("portal.notification.delete.question");
  }

  public String getPortalDeleteNotificationExplanation() {
    return getTranslation("portal.notification.delete.explanation");
  }

  public String getPortalDeleteNotificationHint() {
    return getTranslation("portal.notification.delete.hint");
  }

  public String getPortalDeleteReadExplanation() {
    return getTranslation("portal.notification.delete.read.explanation");
  }

  public String getPortalNotificationAffected(long affectedNotificationCount) {
    return getFormattedTranslation(
        affectedNotificationCount == 1
            ? "portal.notification.affected.singular"
            : "portal.notification.affected.plural",
        affectedNotificationCount);
  }

  public String getPortalDeleteReadQuestion() {
    return getTranslation("portal.notification.delete.read.question");
  }

  public String getPortalMarkReadQuestion() {
    return getTranslation("portal.notification.read.question");
  }

  public String getPortalMarkReadExplanation() {
    return getTranslation("portal.notification.read.explanation");
  }

  public String getPortalMarkAllQuestion() {
    return getTranslation("portal.notification.read.all.question");
  }

  public String getPortalMarkAllExplanation() {
    return getTranslation("portal.notification.read.all.explanation");
  }

  public String getPortalNotificationSyncHint() {
    return getTranslation("portal.notification.sync.hint");
  }

  // Sitzungen und gespeicherte Anmeldungen im Portal
  public String getPortalSessionLogoutTitle() {
    return getTranslation("portal.session.logout.title");
  }

  public String getPortalSessionSavedTitle() {
    return getTranslation("portal.session.saved.title");
  }

  public String getPortalSessionLogoutQuestion() {
    return getTranslation("portal.session.logout.question");
  }

  public String getPortalSessionSavedQuestion() {
    return getTranslation("portal.session.saved.question");
  }

  public String getPortalSessionSavedExplanation() {
    return getTranslation("portal.session.saved.explanation");
  }

  public String getPortalSessionOthersQuestion() {
    return getTranslation("portal.session.others.question");
  }

  public String getPortalSessionRetainedHint() {
    return getTranslation("portal.session.retained.hint");
  }

  public String getPortalPresenceHeading() {
    return getTranslation("portal.menu.presence");
  }

  public String getPortalLanguageHeading() {
    return getTranslation("portal.menu.language");
  }

  public String getPortalPaginationRange(
      long firstVisibleRow, long lastVisibleRow, long totalRowCount) {
    return getFormattedTranslation(
        "portal.pagination.range", firstVisibleRow, lastVisibleRow, totalRowCount);
  }

  public String getPortalConfirmAction() {
    return getTranslation("portal.action.confirm");
  }

  public String getPortalSessionActive() {
    return getTranslation("portal.session.active");
  }

  public String getPortalSessionInactive() {
    return getTranslation("portal.session.inactive");
  }

  public String getPortalSessionActiveGroup(int activeSessionCount) {
    return getFormattedTranslation("portal.session.group.active", activeSessionCount);
  }

  public String getPortalSessionSavedGroup(int savedSignInCount) {
    return getFormattedTranslation("portal.session.group.saved", savedSignInCount);
  }

  public String getPortalSessionActiveCount(int activeSessionCount) {
    return getFormattedTranslation(
        activeSessionCount == 1 ? "portal.session.count.singular" : "portal.session.count.plural",
        activeSessionCount);
  }

  public String getPortalSessionSelection(int activeSessionCount, int savedSignInCount) {
    String savedSignInCountText =
        getFormattedTranslation(
            savedSignInCount == 1
                ? "portal.session.saved.count.singular"
                : "portal.session.saved.count.plural",
            savedSignInCount);

    return getFormattedTranslation(
        "portal.session.selection",
        getPortalSessionActiveCount(activeSessionCount),
        savedSignInCountText);
  }

  public String getPortalSessionCheckSelection() {
    return getTranslation("portal.session.check.selection");
  }

  public String getPortalSessionRevokeAction() {
    return getTranslation("portal.session.revoke.action");
  }

  public String getPortalSessionLastUsed() {
    return getTranslation("portal.session.last.used");
  }

  public String getPortalSessionRemembered() {
    return getTranslation("portal.session.remembered");
  }

  public String getPortalSessionLogoutAria(String deviceName) {
    return getFormattedTranslation("portal.session.logout.aria", deviceName);
  }

  // Öffentliche Seiten und Kontoabläufe
  public String getPublicAreaLabel() {
    return getTranslation("public.area");
  }

  public String getPublicHomeIntroduction() {
    return getTranslation("public.home.introduction");
  }

  public String getPublicHomeDescription() {
    return getTranslation("public.home.description");
  }

  public String getPublicHomeKicker() {
    return getTranslation("public.home.kicker");
  }

  public String getPublicLoginIntroduction() {
    return getTranslation("public.login.introduction");
  }

  public String getPublicLoginSubtitle() {
    return getTranslation("public.login.subtitle");
  }

  public String getPublicRegisterIntroduction() {
    return getTranslation("public.register.introduction");
  }

  public String getPublicRegisterSubtitle() {
    return getTranslation("public.register.subtitle");
  }

  public String getPublicForgotTitle() {
    return getTranslation("public.forgot.title");
  }

  public String getPublicForgotIntroduction() {
    return getTranslation("public.forgot.introduction");
  }

  public String getPublicForgotSubtitle() {
    return getTranslation("public.forgot.subtitle");
  }

  public String getPublicRequestLink() {
    return getTranslation("public.request.link");
  }

  public String getPublicRequestNewLink() {
    return getTranslation("public.request.new.link");
  }

  public String getPublicHomeAction() {
    return getTranslation("public.home.action");
  }

  public String getPublicResetIntroduction() {
    return getTranslation("public.reset.introduction");
  }

  public String getPublicResetSubtitle() {
    return getTranslation("public.reset.subtitle");
  }

  public String getPublicResetFormTitle() {
    return getTranslation("public.reset.form.title");
  }

  public String getPublicSavePasswordAction() {
    return getTranslation("public.reset.save.password");
  }

  public String getPublicVerificationTitle() {
    return getTranslation("public.verification.title");
  }

  public String getPublicVerificationIntroduction() {
    return getTranslation("public.verification.introduction");
  }

  public String getPublicVerificationSubtitle() {
    return getTranslation("public.verification.subtitle");
  }

  public String getPublicEmailIntroduction() {
    return getTranslation("public.email.introduction");
  }

  public String getPublicEmailSubtitle() {
    return getTranslation("public.email.subtitle");
  }

  public String getPublicEmailFormTitle() {
    return getTranslation("public.email.form.title");
  }

  public String getPublicEmailExpiredTitle() {
    return getTranslation("public.email.expired.title");
  }

  public String getPublicEmailInvalidTitle() {
    return getTranslation("public.email.invalid.title");
  }

  public String getPublicEmailConflictTitle() {
    return getTranslation("public.email.conflict.title");
  }

  public String getPublicEmailSuccessTitle() {
    return getTranslation("public.email.success.title");
  }

  public String getPublicEmailConfirmationHint() {
    return getTranslation("public.email.confirmation.hint");
  }

  public String getPublicMaintenanceIntroduction() {
    return getTranslation("public.maintenance.introduction");
  }

  public String getPublicMaintenanceSubtitle() {
    return getTranslation("public.maintenance.subtitle");
  }

  public String getPublicOutcomeSubtitle() {
    return getTranslation("public.outcome.subtitle");
  }

  public String getPublicRecoveryTitle() {
    return getTranslation("public.recovery.title");
  }

  public String getPublicRecoveryIntroduction() {
    return getTranslation("public.recovery.introduction");
  }

  public String getPublicEmailField() {
    return getTranslation("public.field.email");
  }

  public String getPublicConfirmEmailField() {
    return getTranslation("public.field.email.confirm");
  }

  public String getPublicConfirmPasswordField() {
    return getTranslation("public.field.password.confirm");
  }

  // Anmeldeanbieter konfigurieren
  public String getProviderSettingsTitle() {
    return getTranslation("admin.providers.title");
  }

  public String getProviderSettingsDescription() {
    return getTranslation("admin.providers.description");
  }

  public String getProviderSettingsPanelTitle() {
    return getTranslation("admin.providers.panelTitle");
  }

  public String getProviderSettingsPanelDescription() {
    return getTranslation("admin.providers.panelDescription");
  }

  public String getProviderSettingsAssistantKicker() {
    return getTranslation("admin.providers.assistantKicker");
  }

  public String getProviderSettingsAssistantTitle(String providerName) {
    return getFormattedTranslation("admin.providers.assistantTitle", providerName);
  }

  public String getProviderSettingsAssistantDescription() {
    return getTranslation("admin.providers.assistantDescription");
  }

  public String getProviderSettingsSetupCredentials() {
    return getTranslation("admin.providers.setupCredentials");
  }

  public String getProviderSettingsEnableSignIn() {
    return getTranslation("admin.providers.enableSignIn");
  }

  public String getProviderSettingsSetupProvider() {
    return getTranslation("admin.providers.setupProvider");
  }

  public String getProviderSettingsGoogleGuide() {
    return getTranslation("admin.providers.googleGuide");
  }

  public String getProviderSettingsMicrosoftGuide() {
    return getTranslation("admin.providers.microsoftGuide");
  }

  public String getProviderSettingsOpenConsole() {
    return getTranslation("admin.providers.openConsole");
  }

  public String getProviderSettingsRedirect() {
    return getTranslation("admin.providers.redirect");
  }

  public String getProviderSettingsCopy() {
    return getTranslation("admin.providers.copy");
  }

  public String getProviderSettingsCopied() {
    return getTranslation("admin.providers.copied");
  }

  public String getProviderSettingsCopyFailed() {
    return getTranslation("admin.providers.copyFailed");
  }

  public String getProviderSettingsClientId() {
    return getTranslation("admin.providers.clientId");
  }

  public String getProviderSettingsClientIdPlaceholder() {
    return getTranslation("admin.providers.clientIdPlaceholder");
  }

  public String getProviderSettingsSecret() {
    return getTranslation("admin.providers.secret");
  }

  public String getProviderSettingsSecretPlaceholder() {
    return getTranslation("admin.providers.secretPlaceholder");
  }

  public String getProviderSettingsSecretStored() {
    return getTranslation("admin.providers.secretStored");
  }

  public String getProviderSettingsSecretHint() {
    return getTranslation("admin.providers.secretHint");
  }

  public String getProviderSettingsEnable() {
    return getTranslation("admin.providers.enable");
  }

  public String getProviderSettingsHint() {
    return getTranslation("admin.providers.hint");
  }

  public String getProviderSettingsUnconfiguredHint(String providerName) {
    return getFormattedTranslation("admin.providers.unconfiguredHint", providerName);
  }

  public String getProviderSettingsActiveHint(String providerName) {
    return getFormattedTranslation("admin.providers.activeHint", providerName);
  }

  public String getProviderSettingsInactiveHint(String providerName) {
    return getFormattedTranslation("admin.providers.inactiveHint", providerName);
  }

  public String getProviderSettingsClientIdHelp(boolean isMicrosoftProvider) {
    return getTranslation(
        isMicrosoftProvider
            ? "admin.providers.microsoft.clientIdHelp"
            : "admin.providers.google.clientIdHelp");
  }

  public String getProviderSettingsSecretHelp(boolean isMicrosoftProvider) {
    return getTranslation(
        isMicrosoftProvider
            ? "admin.providers.microsoft.secretHelp"
            : "admin.providers.google.secretHelp");
  }

  public String getProviderSettingsEnableHelp() {
    return getTranslation("admin.providers.enableHelp");
  }

  public String getProviderSettingsClientIdRule() {
    return getTranslation("admin.providers.clientIdRule");
  }

  public String getProviderSettingsSecretRule() {
    return getTranslation("admin.providers.secretRule");
  }

  public String getProviderSettingsEnableRule() {
    return getTranslation("admin.providers.enableRule");
  }

  public String getProviderSettingsActive() {
    return getTranslation("admin.providers.active");
  }

  public String getProviderSettingsInactive() {
    return getTranslation("admin.providers.inactive");
  }

  public String getProviderSettingsSaved() {
    return getTranslation("admin.providers.saved");
  }

  public String getProviderSettingsIncomplete() {
    return getTranslation("admin.providers.incomplete");
  }

  public String getProviderSettingsConflict() {
    return getTranslation("admin.providers.conflict");
  }

  public String getProviderSettingsFailed() {
    return getTranslation("admin.providers.failed");
  }

  public String getEmailVerified() {
    return getTranslation("email.verification.verified");
  }

  public String getEmailVerificationPending() {
    return getTranslation("email.verification.pending");
  }

  public String getEmailNotChecked() {
    return getTranslation("email.verification.notChecked");
  }

  public String getEmailNotCheckedHint() {
    return getTranslation("email.verification.notCheckedHint");
  }

  public String getEmailVerificationTitle() {
    return getTranslation("email.verification.title");
  }

  public String getEmailVerificationIntro() {
    return getTranslation("email.verification.intro");
  }

  public String getEmailVerificationSpamHint() {
    return getTranslation("email.verification.spamHint");
  }

  public String getEmailVerificationResend() {
    return getTranslation("email.verification.resend");
  }

  public String getEmailVerificationBack() {
    return getTranslation("email.verification.back");
  }

  public String getEmailVerificationQueued() {
    return getTranslation("email.verification.queued");
  }

  public String getEmailVerificationWait() {
    return getTranslation("email.verification.wait");
  }

  public String getAdminEmailVerificationRequired() {
    return getTranslation("email.verification.required");
  }

  public String getAdminEmailVerificationHint() {
    return getTranslation("email.verification.requiredHint");
  }

  public String getAdminPasswordResetMail() {
    return getTranslation("email.verification.reset");
  }

  public String getRegistrationAdministration() {
    return getTranslation("registration.channel.administration");
  }

  public String getRegistrationApplicationStart() {
    return getTranslation("registration.channel.start");
  }

  public String getEmailVerificationLabel() {
    return getTranslation("email.verification.label");
  }

  public String getAdminAccountMailActionsHint() {
    return getTranslation("admin.users.accountMailActionsHint");
  }

  public String getAdminSendVerificationLink() {
    return getTranslation("admin.users.sendVerificationLink");
  }
}
