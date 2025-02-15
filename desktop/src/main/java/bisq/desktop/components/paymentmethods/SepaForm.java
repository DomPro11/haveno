/*
 * This file is part of Haveno.
 *
 * Haveno is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Haveno is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Haveno. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.components.paymentmethods;

import static bisq.desktop.util.FormBuilder.addCompactTopLabelTextField;
import static bisq.desktop.util.FormBuilder.addCompactTopLabelTextFieldWithCopyIcon;

import bisq.core.account.witness.AccountAgeWitnessService;
import bisq.core.locale.Country;
import bisq.core.locale.CountryUtil;
import bisq.core.locale.Res;
import bisq.core.locale.TradeCurrency;
import bisq.core.payment.PaymentAccount;
import bisq.core.payment.SepaAccount;
import bisq.core.payment.payload.PaymentAccountPayload;
import bisq.core.payment.payload.SepaAccountPayload;
import bisq.core.payment.validation.BICValidator;
import bisq.core.payment.validation.SepaIBANValidator;
import bisq.core.util.coin.CoinFormatter;
import bisq.core.util.validation.InputValidator;
import bisq.desktop.components.InputTextField;
import bisq.desktop.util.FormBuilder;
import bisq.desktop.util.normalization.IBANNormalizer;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;

public class SepaForm extends GeneralSepaForm {

    public static int addFormForBuyer(GridPane gridPane, int gridRow,
                                      PaymentAccountPayload paymentAccountPayload) {
        SepaAccountPayload sepaAccountPayload = (SepaAccountPayload) paymentAccountPayload;

        final String title = Res.get("payment.account.owner");
        final String value = sepaAccountPayload.getHolderName();
        addCompactTopLabelTextFieldWithCopyIcon(gridPane, ++gridRow, title, value);

        addCompactTopLabelTextFieldWithCopyIcon(gridPane, gridRow, 1,
                Res.get("payment.bank.country"),
                CountryUtil.getNameAndCode(sepaAccountPayload.getCountryCode()));
        // IBAN, BIC will not be translated
        addCompactTopLabelTextFieldWithCopyIcon(gridPane, ++gridRow, IBAN, sepaAccountPayload.getIban());
        addCompactTopLabelTextFieldWithCopyIcon(gridPane, gridRow, 1, BIC, sepaAccountPayload.getBic());
        return gridRow;
    }

    private final SepaAccount sepaAccount;
    private final SepaIBANValidator sepaIBANValidator;
    private final BICValidator bicValidator;

    public SepaForm(PaymentAccount paymentAccount,
                    AccountAgeWitnessService accountAgeWitnessService,
                    BICValidator bicValidator,
                    InputValidator inputValidator,
                    GridPane gridPane,
                    int gridRow,
                    CoinFormatter formatter) {
        super(paymentAccount, accountAgeWitnessService, inputValidator, gridPane, gridRow, formatter);
        this.sepaAccount = (SepaAccount) paymentAccount;
        this.bicValidator = bicValidator;
        this.sepaIBANValidator = new SepaIBANValidator();
    }

    @Override
    public void addFormForAddAccount() {
        gridRowFrom = gridRow + 1;

        InputTextField holderNameInputTextField = FormBuilder.addInputTextField(gridPane, ++gridRow,
                Res.get("payment.account.owner"));
        holderNameInputTextField.setValidator(inputValidator);
        holderNameInputTextField.textProperty().addListener((ov, oldValue, newValue) -> {
            sepaAccount.setHolderName(newValue);
            updateFromInputs();
        });

        InputTextField ibanInputTextField = FormBuilder.addInputTextField(gridPane, ++gridRow, IBAN);
        ibanInputTextField.setTextFormatter(new TextFormatter<>(new IBANNormalizer()));
        ibanInputTextField.setValidator(sepaIBANValidator);

        InputTextField bicInputTextField = FormBuilder.addInputTextField(gridPane, ++gridRow, BIC);
        bicInputTextField.setValidator(bicValidator);
        bicInputTextField.textProperty().addListener((ov, oldValue, newValue) -> {
            sepaAccount.setBic(newValue);
            updateFromInputs();

        });

        ComboBox<Country> countryComboBox = addCountrySelection();

        setCountryComboBoxAction(countryComboBox, sepaAccount);

        addCountriesGrid(Res.get("payment.accept.euro"), CountryUtil.getAllSepaEuroCountries());
        addCountriesGrid(Res.get("payment.accept.nonEuro"), CountryUtil.getAllSepaNonEuroCountries());
        addLimitations(false);
        addAccountNameTextFieldWithAutoFillToggleButton();

        countryComboBox.setItems(FXCollections.observableArrayList(CountryUtil.getAllSepaCountries()));
        Country country = CountryUtil.getDefaultCountry();
        if (CountryUtil.getAllSepaCountries().contains(country)) {
            countryComboBox.getSelectionModel().select(country);
            sepaAccount.setCountry(country);
        }

        ibanInputTextField.textProperty().addListener((ov, oldValue, newValue) -> {
            sepaAccount.setIban(newValue);
            updateFromInputs();

            if (ibanInputTextField.validate()) {
                List<Country> countries = CountryUtil.getAllSepaCountries();
                String ibanCountryCode = newValue.substring(0, 2).toUpperCase();
                Optional<Country> ibanCountry = countries
                        .stream()
                        .filter(c -> c.code.equals(ibanCountryCode))
                        .findFirst();

                ibanCountry.ifPresent(countryComboBox::setValue);
            }
        });

        countryComboBox.valueProperty().addListener((ov, oldValue, newValue) -> {
            sepaIBANValidator.setRestrictToCountry(newValue.code);
            ibanInputTextField.refreshValidation();
        });

        updateFromInputs();
    }

    @Override
    public void updateAllInputsValid() {
        allInputsValid.set(isAccountNameValid()
                && bicValidator.validate(sepaAccount.getBic()).isValid
                && sepaIBANValidator.validate(sepaAccount.getIban()).isValid
                && inputValidator.validate(sepaAccount.getHolderName()).isValid
                && sepaAccount.getAcceptedCountryCodes().size() > 0
                && sepaAccount.getSingleTradeCurrency() != null
                && sepaAccount.getCountry() != null);
    }

    @Override
    public void addFormForEditAccount() {
        gridRowFrom = gridRow;
        addAccountNameTextFieldWithAutoFillToggleButton();
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("shared.paymentMethod"),
                Res.get(sepaAccount.getPaymentMethod().getId()));
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("payment.account.owner"), sepaAccount.getHolderName());
        addCompactTopLabelTextField(gridPane, ++gridRow, IBAN, sepaAccount.getIban()).second.setMouseTransparent(false);
        addCompactTopLabelTextField(gridPane, ++gridRow, BIC, sepaAccount.getBic()).second.setMouseTransparent(false);
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("payment.bank.country"),
                sepaAccount.getCountry() != null ? sepaAccount.getCountry().name : "");
        TradeCurrency singleTradeCurrency = sepaAccount.getSingleTradeCurrency();
        String nameAndCode = singleTradeCurrency != null ? singleTradeCurrency.getNameAndCode() : "null";
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("shared.currency"), nameAndCode);

        addCountriesGrid(Res.get("payment.accept.euro"), CountryUtil.getAllSepaEuroCountries());
        addCountriesGrid(Res.get("payment.accept.nonEuro"), CountryUtil.getAllSepaNonEuroCountries());
        addLimitations(true);
    }

    @Override
    void removeAcceptedCountry(String countryCode) {
        sepaAccount.removeAcceptedCountry(countryCode);
    }

    @Override
    void addAcceptedCountry(String countryCode) {
        sepaAccount.addAcceptedCountry(countryCode);
    }

    @Override
    boolean isCountryAccepted(String countryCode) {
        return sepaAccount.getAcceptedCountryCodes().contains(countryCode);
    }

    @Override
    protected String getIban() {
        return sepaAccount.getIban();
    }
}
