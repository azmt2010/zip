package com.qtyy.admin

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent as composeActivitySetContent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Keeps MainActivity terse while delegating to Activity Compose's real setContent extension.
 */
fun ComponentActivity.setContent(content: @Composable () -> Unit) {
    this.composeActivitySetContent(content = content)
}

/**
 * Compatibility overload matching the app's compact AdminField call shape.
 */
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelContent: @Composable () -> Unit,
    modifier: Modifier,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape,
    colors: TextFieldColors,
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = labelContent,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
    )
}
