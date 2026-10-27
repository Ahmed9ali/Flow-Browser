package com.example.automation

object AutomationScripts {

    /**
     * JavaScript injected to record user interactions on the page.
     * Hooks into click, input, select, and change events and forwards them to Android.
     */
    val RECORDING_INJECTION = """
        (function() {
            if (window._auraRecorderActive) return;
            window._auraRecorderActive = true;

            function getCssSelector(el) {
                if (!el || el === document) return '';
                if (el.id) return '#' + el.id;
                if (el.name) return el.tagName.toLowerCase() + '[name="' + el.name + '"]';
                if (el.getAttribute('data-testid')) return '[data-testid="' + el.getAttribute('data-testid') + '"]';
                if (el.getAttribute('aria-label')) return '[aria-label="' + el.getAttribute('aria-label') + '"]';
                
                var path = [];
                var curr = el;
                while (curr && curr.nodeType === Node.ELEMENT_NODE && curr !== document.body && curr !== document.documentElement) {
                    var selector = curr.tagName.toLowerCase();
                    if (curr.id) {
                        path.unshift('#' + curr.id);
                        break;
                    }
                    if (curr.className && typeof curr.className === 'string') {
                        var classes = curr.className.trim().split(/\s+/).filter(function(c) { 
                            return c && !c.startsWith('aura-') && !c.includes(':'); 
                        });
                        if (classes.length > 0) selector += '.' + classes[0];
                    }
                    path.unshift(selector);
                    curr = curr.parentNode;
                }
                return path.join(' > ');
            }

            function getElementDescription(el) {
                if (!el) return 'Element';
                var text = (el.innerText || el.textContent || el.value || el.placeholder || '').trim();
                if (text.length > 30) text = text.substring(0, 30) + '...';
                var tag = el.tagName.toLowerCase();
                if (tag === 'button' || el.type === 'submit') return 'Button: ' + (text || 'Submit');
                if (tag === 'input') return 'Input (' + (el.type || 'text') + '): ' + (el.placeholder || el.name || text);
                if (tag === 'select') return 'Dropdown: ' + (el.name || text || 'Select');
                if (tag === 'a') return 'Link: ' + text;
                return tag + (text ? ' "' + text + '"' : '');
            }

            document.addEventListener('click', function(e) {
                var el = e.target;
                if (!el) return;
                if (el.closest && el.closest('.aura-hud')) return;

                var selector = getCssSelector(el);
                var desc = getElementDescription(el);
                
                if (window.AuraAutomation && window.AuraAutomation.onUserAction) {
                    window.AuraAutomation.onUserAction('CLICK', selector, desc, '');
                }
            }, true);

            document.addEventListener('change', function(e) {
                var el = e.target;
                if (!el) return;
                if (el.closest && el.closest('.aura-hud')) return;

                var tag = el.tagName.toLowerCase();
                if (tag === 'input' || tag === 'textarea' || tag === 'select') {
                    var selector = getCssSelector(el);
                    var desc = getElementDescription(el);
                    var val = el.value || '';
                    var action = (tag === 'select') ? 'SELECT_DROPDOWN' : 'INPUT';
                    if (el.type === 'file') action = 'UPLOAD_FILE';
                    if (el.type === 'date') action = 'SELECT_DATE';

                    if (window.AuraAutomation && window.AuraAutomation.onUserAction) {
                        window.AuraAutomation.onUserAction(action, selector, desc, val);
                    }
                }
            }, true);
        })();
    """.trimIndent()

    /**
     * Modern DOM Scanner:
     * Scans and semantically identifies:
     * - Email, Password, Confirm Password
     * - First Name, Last Name, Full Name
     * - Phone Number & Country Code (selectors, bottom sheets, dial codes)
     * - Gender Dropdowns (select, radio, custom comboboxes)
     * - Date of Birth (DOB) Calendar & Date Pickers (input[type="date"], split selects, popups)
     * - Address, Address Line 2, City, State, ZIP / PIN Code
     * - File Upload fields (input[type="file"], upload dropzones)
     * - Terms & Policy Checkboxes
     * - Submit / Continue / Create Account Buttons
     */
    val DOM_SCANNER = """
        (function() {
            var items = [];
            var elements = document.querySelectorAll(
                'input, button, [role="button"], [role="combobox"], [role="listbox"], textarea, select, form, a.btn, .iti__selected-flag, [data-dial-code], [data-datepicker], .dropzone'
            );

            function getCssSelector(el) {
                if (el.id) return '#' + el.id;
                if (el.name) return el.tagName.toLowerCase() + '[name="' + el.name + '"]';
                if (el.getAttribute('data-testid')) return '[data-testid="' + el.getAttribute('data-testid') + '"]';
                if (el.getAttribute('aria-label')) return '[aria-label="' + el.getAttribute('aria-label') + '"]';
                if (el.className && typeof el.className === 'string') {
                    var classes = el.className.trim().split(/\s+/).filter(function(c) {
                        return c && !c.startsWith('aura-') && !c.includes(':');
                    });
                    if (classes.length > 0) return el.tagName.toLowerCase() + '.' + classes[0];
                }
                return el.tagName.toLowerCase();
            }

            function classifySemanticType(el, tag, type, name, id, placeholder, text, ariaLabel) {
                var combined = (name + ' ' + id + ' ' + placeholder + ' ' + text + ' ' + ariaLabel + ' ' + (el.autocomplete || '')).toLowerCase();

                // 1. File Upload
                if (type === 'file' || /upload|resume|document|avatar|attachment|file|cv|browse/i.test(combined)) {
                    if (tag === 'input' && type === 'file') return 'FILE_UPLOAD';
                    if (/upload|attach|choose file|browse/i.test(combined)) return 'FILE_UPLOAD';
                }

                // 2. Passwords: Distinguish Confirm Password from Primary Password
                if (type === 'password' || /pass/i.test(combined)) {
                    if (/confirm|repeat|retype|re-enter|verify|match|pass.*2/i.test(combined)) {
                        return 'CONFIRM_PASSWORD';
                    }
                    return 'PASSWORD';
                }

                // 3. Email
                if (type === 'email' || /email|e-mail|mail/i.test(combined)) {
                    return 'EMAIL';
                }

                // 4. Country Code (dropdowns, intl-tel-input, dial code pickers)
                if (/country.*code|dial.*code|phone.*prefix|dialing|iti__selected-flag/i.test(combined) || 
                    el.classList.contains('iti__selected-flag') || 
                    el.hasAttribute('data-dial-code')) {
                    return 'COUNTRY_CODE';
                }

                // 5. Phone Number
                if (type === 'tel' || /phone|mobile|cell|telephone|contact.*num/i.test(combined)) {
                    return 'PHONE';
                }

                // 6. Gender (Dropdown, Radios, Comboboxes)
                if (/gender|sex/i.test(combined) || 
                    (tag === 'select' && /male|female/i.test(el.innerText || '')) ||
                    (type === 'radio' && /male|female/i.test(combined))) {
                    return 'GENDER';
                }

                // 7. Date of Birth (DOB) Calendar & Date Pickers
                if (type === 'date' || type === 'datetime-local' || /birth|dob|bday|date.*of.*birth/i.test(combined) || el.hasAttribute('data-datepicker')) {
                    if (/day/i.test(combined)) return 'DOB_DAY';
                    if (/month/i.test(combined)) return 'DOB_MONTH';
                    if (/year/i.test(combined)) return 'DOB_YEAR';
                    return 'DOB';
                }
                if (/^(day|birth_day|dob_day)$/i.test(name) || /^(day)$/i.test(id)) return 'DOB_DAY';
                if (/^(month|birth_month|dob_month)$/i.test(name) || /^(month)$/i.test(id)) return 'DOB_MONTH';
                if (/^(year|birth_year|dob_year)$/i.test(name) || /^(year)$/i.test(id)) return 'DOB_YEAR';

                // 8. Name Fields (First, Last, Full)
                if (/first.*name|fname|given.*name/i.test(combined)) return 'FIRST_NAME';
                if (/last.*name|lname|surname|family.*name/i.test(combined)) return 'LAST_NAME';
                if (/full.*name|^name$|your.*name/i.test(combined) && tag === 'input') return 'FULL_NAME';

                // 9. Postal / PIN / ZIP Code
                if (/zip|postal|pincode|pin.*code|postcode/i.test(combined)) return 'ZIP_PIN';

                // 10. Address, City, State
                if (/city|town|locality/i.test(combined)) return 'CITY';
                if (/state|province|region/i.test(combined)) return 'STATE';
                if (/address.*2|line.*2|apt|suite|unit/i.test(combined)) return 'ADDRESS_LINE_2';
                if (/address|street|addr/i.test(combined)) return 'ADDRESS';

                // 11. Policy & Terms Checkbox
                if (type === 'checkbox' && /term|agree|policy|accept|consent|privacy/i.test(combined)) {
                    return 'CHECKBOX_TERMS';
                }

                // 12. Submit / Next / Action Buttons
                if (tag === 'button' || type === 'submit' || el.getAttribute('role') === 'button' || tag === 'a') {
                    if (/submit|sign.*up|create.*account|register|finish|done/i.test(combined)) return 'SUBMIT_BUTTON';
                    if (/next|continue|proceed|forward/i.test(combined)) return 'NEXT_BUTTON';
                    return 'GENERIC_BUTTON';
                }

                // 13. Dropdown / Combobox
                if (tag === 'select' || el.getAttribute('role') === 'combobox' || el.getAttribute('role') === 'listbox') {
                    return 'DROPDOWN';
                }

                return tag === 'input' || tag === 'textarea' ? 'GENERIC_INPUT' : 'GENERIC_BUTTON';
            }

            elements.forEach(function(el) {
                if (el.type === 'hidden' || el.style.display === 'none' || el.style.visibility === 'hidden') return;
                if (el.closest && el.closest('.aura-hud')) return;

                var tag = el.tagName.toLowerCase();
                var type = el.type || '';
                var name = el.name || '';
                var id = el.id || '';
                var placeholder = el.placeholder || '';
                var ariaLabel = el.getAttribute('aria-label') || '';
                var text = (el.innerText || el.textContent || el.value || '').trim();
                if (text.length > 35) text = text.substring(0, 35) + '...';

                var isInput = (tag === 'input' || tag === 'textarea' || tag === 'select' || el.getAttribute('role') === 'combobox');
                var isBtn = (tag === 'button' || type === 'submit' || el.getAttribute('role') === 'button' || tag === 'a');

                var semantic = classifySemanticType(el, tag, type, name, id, placeholder, text, ariaLabel);

                // Highlight interactable elements with glowing outline
                el.style.outline = '2px dashed #06B6D4';
                el.style.outlineOffset = '2px';

                // Collect options for select / dropdowns
                var options = [];
                if (tag === 'select' && el.options) {
                    for (var o = 0; o < Math.min(el.options.length, 12); o++) {
                        var optText = (el.options[o].text || el.options[o].value || '').trim();
                        if (optText) options.push(optText);
                    }
                }

                items.push({
                    tagName: tag,
                    type: type,
                    nameAttr: name,
                    idAttr: id,
                    placeholder: placeholder,
                    text: text,
                    selector: getCssSelector(el),
                    isInput: isInput,
                    isButton: isBtn,
                    semanticType: semantic,
                    detectedOptions: options
                });
            });

            if (window.AuraAutomation && window.AuraAutomation.onScanResults) {
                window.AuraAutomation.onScanResults(JSON.stringify(items));
            }
        })();
    """.trimIndent()

    /**
     * Clear DOM scan visual outlines.
     */
    val CLEAR_SCAN_HIGHLIGHTS = """
        (function() {
            var elements = document.querySelectorAll(
                'input, button, [role="button"], [role="combobox"], [role="listbox"], textarea, select, form, a.btn, .iti__selected-flag, [data-dial-code], [data-datepicker], .dropzone'
            );
            elements.forEach(function(el) {
                el.style.outline = '';
                el.style.outlineOffset = '';
            });
        })();
    """.trimIndent()

    /**
     * Simulate a human click on an element by CSS selector or fallback text.
     */
    fun createClickScript(selector: String): String {
        val escaped = selector.replace("'", "\\'")
        return """
            (function() {
                var el = document.querySelector('$escaped');
                if (!el) {
                    var buttons = document.querySelectorAll('button, [role="button"], input[type="submit"], a');
                    for (var i = 0; i < buttons.length; i++) {
                        if ((buttons[i].innerText || buttons[i].value || '').trim().toLowerCase().indexOf('$escaped'.toLowerCase()) >= 0) {
                            el = buttons[i];
                            break;
                        }
                    }
                }
                if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    el.focus();
                    ['mousedown', 'click', 'mouseup'].forEach(function(evt) {
                        var event = new MouseEvent(evt, { bubbles: true, cancelable: true, view: window });
                        el.dispatchEvent(event);
                    });
                    if (el.tagName.toLowerCase() === 'input' && (el.type === 'checkbox' || el.type === 'radio')) {
                        el.checked = !el.checked;
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                    return 'SUCCESS';
                }
                return 'NOT_FOUND';
            })();
        """.trimIndent()
    }

    /**
     * Input text into modern fields with full event dispatching for React, Vue, Angular, and controlled inputs.
     */
    fun createInputScript(selector: String, value: String, clearFirst: Boolean): String {
        val escapedSelector = selector.replace("'", "\\'")
        val escapedValue = value.replace("'", "\\'").replace("\n", "\\n")
        return """
            (function() {
                var el = document.querySelector('$escapedSelector');
                if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    el.focus();

                    // Native value setter to bypass React controlled input interception
                    var nativeSetter = Object.getOwnPropertyDescriptor(
                        el.tagName.toLowerCase() === 'textarea' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype,
                        'value'
                    );

                    if ($clearFirst) {
                        if (nativeSetter && nativeSetter.set) {
                            nativeSetter.set.call(el, '');
                        } else {
                            el.value = '';
                        }
                        if (el._valueTracker) el._valueTracker.setValue('');
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                    }

                    if (nativeSetter && nativeSetter.set) {
                        nativeSetter.set.call(el, '$escapedValue');
                    } else {
                        el.value = '$escapedValue';
                    }

                    if (el._valueTracker) {
                        el._valueTracker.setValue('$escapedValue');
                    }

                    // Dispatch complete reactive pipeline
                    el.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key: 'a' }));
                    el.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));
                    el.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true, key: 'a' }));
                    el.dispatchEvent(new Event('change', { bubbles: true, cancelable: true }));
                    el.dispatchEvent(new Event('blur', { bubbles: true }));
                    return 'SUCCESS';
                }
                return 'NOT_FOUND';
            })();
        """.trimIndent()
    }

    /**
     * Modern Dropdown Handler:
     * Supports standard <select> elements, custom comboboxes, and listbox components (Gender, Options, etc.).
     */
    fun createSelectDropdownScript(selector: String, optionValueOrText: String): String {
        val escapedSelector = selector.replace("'", "\\'")
        val escapedTarget = optionValueOrText.replace("'", "\\'")
        return """
            (function() {
                var el = document.querySelector('$escapedSelector');
                if (!el) return 'NOT_FOUND';

                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                el.focus();

                var target = '$escapedTarget'.toLowerCase().trim();

                // 1. Standard HTML5 <select>
                if (el.tagName.toLowerCase() === 'select') {
                    var found = false;
                    for (var i = 0; i < el.options.length; i++) {
                        var optText = (el.options[i].text || '').toLowerCase().trim();
                        var optVal = (el.options[i].value || '').toLowerCase().trim();
                        if (optText === target || optVal === target || optText.indexOf(target) >= 0 || optVal.indexOf(target) >= 0) {
                            el.selectedIndex = i;
                            el.value = el.options[i].value;
                            found = true;
                            break;
                        }
                    }
                    if (!found && el.options.length > 1) {
                        el.selectedIndex = 1;
                    }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    return 'SUCCESS';
                }

                // 2. Custom Combobox / Div Dropdown / Bottom Sheet
                // Click trigger to open dropdown
                el.click();
                setTimeout(function() {
                    var options = document.querySelectorAll('[role="option"], li, .dropdown-item, .select-option, [data-value]');
                    for (var j = 0; j < options.length; j++) {
                        var itemText = (options[j].innerText || options[j].textContent || options[j].getAttribute('data-value') || '').toLowerCase().trim();
                        if (itemText === target || itemText.indexOf(target) >= 0) {
                            options[j].click();
                            break;
                        }
                    }
                }, 300);

                return 'SUCCESS';
            })();
        """.trimIndent()
    }

    /**
     * Modern Country Code & Phone Selector:
     * Handles intl-tel-input, custom country pickers, bottom sheets, and standard dial code selectors.
     */
    fun createSelectCountryCodeScript(selector: String, dialCode: String): String {
        val escapedSelector = selector.replace("'", "\\'")
        val cleanCode = dialCode.replace("+", "").trim()
        return """
            (function() {
                var el = document.querySelector('$escapedSelector') || 
                         document.querySelector('.iti__selected-flag, [data-dial-code], [name*="country_code"], #country_code, select[name*="code"]');
                
                if (!el) return 'NOT_FOUND';
                el.scrollIntoView({ behavior: 'smooth', block: 'center' });

                // If standard select element
                if (el.tagName.toLowerCase() === 'select') {
                    for (var i = 0; i < el.options.length; i++) {
                        var text = (el.options[i].text + ' ' + el.options[i].value).toLowerCase();
                        if (text.indexOf('$cleanCode') >= 0 || text.indexOf('+$cleanCode') >= 0) {
                            el.selectedIndex = i;
                            el.value = el.options[i].value;
                            el.dispatchEvent(new Event('change', { bubbles: true }));
                            return 'SUCCESS';
                        }
                    }
                }

                // If custom bottom-sheet or dropdown trigger (e.g. flag icon or button)
                el.click();
                setTimeout(function() {
                    // Search in popup / modal / bottom sheet
                    var items = document.querySelectorAll('.iti__country, [data-dial-code], [role="option"], li');
                    for (var k = 0; k < items.length; k++) {
                        var codeAttr = items[k].getAttribute('data-dial-code') || '';
                        var itemText = (items[k].innerText || items[k].textContent || '').trim();
                        if (codeAttr === '$cleanCode' || itemText.indexOf('+$cleanCode') >= 0) {
                            items[k].click();
                            break;
                        }
                    }
                }, 350);

                return 'SUCCESS';
            })();
        """.trimIndent()
    }

    /**
     * Modern Date of Birth (DOB) Handler:
     * Fills HTML5 date pickers, split selects (Day, Month, Year), or clicks calendar popup date cells.
     */
    fun createSelectDateScript(selector: String, isoDate: String, day: String, month: String, year: String): String {
        val escapedSelector = selector.replace("'", "\\'")
        return """
            (function() {
                var el = document.querySelector('$escapedSelector') || 
                         document.querySelector('input[type="date"], [data-datepicker], input[name*="dob"], input[name*="birth"]');

                if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    el.focus();

                    // 1. Direct HTML5 Date Input
                    if (el.tagName.toLowerCase() === 'input' && el.type === 'date') {
                        el.value = '$isoDate';
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                        return 'SUCCESS';
                    }

                    // 2. Text-based Date Input
                    if (el.tagName.toLowerCase() === 'input') {
                        el.value = '$isoDate';
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                    }

                    // 3. Try clicking calendar picker cell if modal popped up
                    el.click();
                    setTimeout(function() {
                        var dayCells = document.querySelectorAll('[role="gridcell"], .day, .calendar-day, td[data-day]');
                        for (var i = 0; i < dayCells.length; i++) {
                            if ((dayCells[i].innerText || '').trim() === '$day') {
                                dayCells[i].click();
                                break;
                            }
                        }
                    }, 300);

                    return 'SUCCESS';
                }

                // 4. Split Dropdown Fallback (Day, Month, Year)
                var dayEl = document.querySelector('select[name*="day"], #day');
                var monthEl = document.querySelector('select[name*="month"], #month');
                var yearEl = document.querySelector('select[name*="year"], #year');

                if (dayEl) {
                    dayEl.value = '$day';
                    dayEl.dispatchEvent(new Event('change', { bubbles: true }));
                }
                if (monthEl) {
                    monthEl.value = '$month';
                    monthEl.dispatchEvent(new Event('change', { bubbles: true }));
                }
                if (yearEl) {
                    yearEl.value = '$year';
                    yearEl.dispatchEvent(new Event('change', { bubbles: true }));
                }

                return 'SUCCESS';
            })();
        """.trimIndent()
    }

    /**
     * Modern File Upload Automation:
     * Injects synthetic File/Blob into <input type="file"> via DataTransfer API and triggers events.
     */
    fun createFileUploadScript(selector: String, fileName: String, mimeType: String = "application/pdf"): String {
        val escapedSelector = selector.replace("'", "\\'")
        val escapedFileName = fileName.replace("'", "\\'")
        return """
            (function() {
                var el = document.querySelector('$escapedSelector') || document.querySelector('input[type="file"]');
                if (!el) {
                    // Try to find dropzone or upload button
                    var dropzone = document.querySelector('.dropzone, [data-upload], button[aria-label*="upload" i]');
                    if (dropzone) {
                        dropzone.click();
                        return 'CLICKED_DROPZONE';
                    }
                    return 'NOT_FOUND';
                }

                try {
                    var payload = "Aura Browser Automation Payload - " + '$escapedFileName';
                    var blob = new Blob([payload], { type: '$mimeType' });
                    var file = new File([blob], '$escapedFileName', { type: '$mimeType', lastModified: Date.now() });

                    var dt = new DataTransfer();
                    dt.items.add(file);
                    el.files = dt.files;

                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    return 'SUCCESS';
                } catch (e) {
                    return 'ERROR: ' + e.message;
                }
            })();
        """.trimIndent()
    }

    /**
     * Form Submission / Next action.
     */
    fun createSubmitScript(selector: String): String {
        val escaped = selector.replace("'", "\\'")
        return """
            (function() {
                var el = document.querySelector('$escaped') || 
                         document.querySelector('button[type="submit"], input[type="submit"], button.btn-primary');
                if (el) {
                    if (el.tagName.toLowerCase() === 'form') {
                        el.submit();
                        return 'SUCCESS';
                    }
                    if (el.form) {
                        el.form.submit();
                        return 'SUCCESS';
                    }
                    el.click();
                    return 'SUCCESS';
                }
                return 'NOT_FOUND';
            })();
        """.trimIndent()
    }
}
