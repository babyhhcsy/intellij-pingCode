package com.pingCode.ui.util

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/util/DialogValidationUtils.kt
 * @author JetBrains s.r.o.
 */
object DialogValidationUtils {
  /**
   * Returns [ValidationInfo] with [message] if [textField] is blank
   */
  fun notBlank(textField: JTextField, message: String): ValidationInfo? {
    return if (textField.text.isNullOrBlank()) ValidationInfo(message, textField) else null
  }

  /**
   * Chains the [validators] so that if one of them returns non-null [ValidationInfo] the rest of them are not checked
   */
  fun chain(vararg validators: Validator): Validator = { validators.asSequence().mapNotNull { it() }.firstOrNull() }

  /**
   * Stateful validator that checks that contents of [textField] are unique among [records]
   */
  class RecordUniqueValidator(private val textField: JTextField, private val message: String) : Validator {
    var records: Set<String> = setOf()

    override fun invoke(): ValidationInfo? = if (records.contains(textField.text)) ValidationInfo(message, textField) else null
  }
}