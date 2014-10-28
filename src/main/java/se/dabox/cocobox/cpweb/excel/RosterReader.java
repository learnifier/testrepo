/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.excel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.POIXMLException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.util.ParamUtil;
import se.dabox.util.email.EmailValidator;
import se.dabox.util.email.SimpleEmailValidator;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class RosterReader {
    private static final int GIVEN_NAME_MAXLEN = 64;
    private static final int SURNAME_MAXLEN = 64;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RosterReader.class);
    private static final int SHEET_INDEX = 0;
    private InputStream is;
    private Workbook workbook;
    private int startRow = 2;
    private int givenNameCol = 0;
    private int surnameCol = 1;
    private int emailCol = 2;
    private final List<RosterError> errors = new ArrayList<>();
    private final List<Contact> contacts = new ArrayList<>();
    private EmailValidator emailValidator = SimpleEmailValidator.getInstance();

    public List<Contact> readContacts(InputStream inputStream) {
        ParamUtil.required(inputStream, "inputStream");

        this.is = inputStream;
        openWorkbook();
        readSheet();

        return contacts;
    }

    public List<RosterError> getErrors() {
        return errors;
    }

    public boolean isErrorsAvailable() {
        return !errors.isEmpty();
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public int getGivenNameCol() {
        return givenNameCol;
    }

    public void setGivenNameCol(int givenNameCol) {
        this.givenNameCol = givenNameCol;
    }

    public int getSurnameCol() {
        return surnameCol;
    }

    public void setSurnameCol(int surnameCol) {
        this.surnameCol = surnameCol;
    }

    public int getEmailCol() {
        return emailCol;
    }

    public void setEmailCol(int emailCol) {
        this.emailCol = emailCol;
    }

    public EmailValidator getEmailValidator() {
        return emailValidator;
    }

    public void setEmailValidator(EmailValidator emailValidator) {
        this.emailValidator = emailValidator;
    }

    private void openWorkbook() {
        try {
            LOGGER.debug("Trying to open workbook from stream: {}", is);
            workbook = WorkbookFactory.create(is);
        } catch(EncryptedDocumentException ex) {
            throw new RosterFormatException("Can't open an encrypted workbook.", ex);
        } catch (IOException | InvalidFormatException | POIXMLException | IllegalArgumentException ex) {
            throw new RosterFormatException("Failed to read workbook.", ex);
        }
    }

    private void readSheet() {
        Sheet sheet = workbook.getSheetAt(SHEET_INDEX);
        LOGGER.debug("Using sheet {}/{}", SHEET_INDEX, sheet.getSheetName());

        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            Contact contact = readRowContact(row, i);
            if (contact == null) {
                continue;
            }

            contacts.add(contact);
        }
    }

    private Contact readRowContact(Row row, int rowIndex) {
        String gName = getCellValue(row, givenNameCol);
        String sName = getCellValue(row, surnameCol);
        String email = getCellValue(row, emailCol);

        if (allEmpty(gName, sName, email)) {
            return null;
        }

        if (StringUtils.isEmpty(gName)) {
            CellCoordinate coord =
                    new CellCoordinate(rowIndex, givenNameCol);
            addError(new RosterError(coord, "givenname.missing", "Given name is missing"));
            return null;
        }

        if (StringUtils.length(gName) > GIVEN_NAME_MAXLEN) {
            CellCoordinate coord =
                    new CellCoordinate(rowIndex, givenNameCol);
            addError(new RosterError(coord, "givenname.toolong", "Given name too long"));
            return null;
        }

        if (StringUtils.isEmpty(sName)) {
            CellCoordinate coord =
                    new CellCoordinate(rowIndex, surnameCol);
            addError(new RosterError(coord, "surname.missing", "Surname is missing"));
            return null;
        }

        if (StringUtils.length(sName) > SURNAME_MAXLEN) {
            CellCoordinate coord =
                    new CellCoordinate(rowIndex, surnameCol);
            addError(new RosterError(coord, "surname.toolong", "Surname too long"));
            return null;
        }

        if (StringUtils.isEmpty(email)) {
            CellCoordinate coord =
                    new CellCoordinate(rowIndex, emailCol);
            addError(new RosterError(coord, "email.missing", "Email is missing"));
            return null;
        }

        if (!emailValidator.isValidEmail(email)) {
            CellCoordinate coord =
                    new CellCoordinate(rowIndex, emailCol);
            addError(new RosterError(coord, "email.invalid", "Email is invalid", email));
            return null;
        }

        return new Contact(gName, sName, email);
    }

    private String getCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.RETURN_BLANK_AS_NULL);

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            return Double.toString(cell.getNumericCellValue());
        }

        if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
            LOGGER.warn("Formula cell in spreadsheet {}:{}", row.getRowNum(), colIndex);
            return null;
        }


        if (cell.getCellType() != Cell.CELL_TYPE_STRING) {
            //Return invalid value?
        }

        return StringUtils.trim(cell.getStringCellValue());
    }

    private static boolean allEmpty(String... strings) {
        for (String string : strings) {
            if (!StringUtils.isEmpty(string)) {
                return false;
            }
        }

        return true;
    }

    private void addError(RosterError rosterError) {
        errors.add(rosterError);
    }
}
