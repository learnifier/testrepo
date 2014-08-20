/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.io.IOException;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.request.BytesRequestTarget;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountTransformers;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class ExcelRoster {

    private final RequestCycle cycle;

    public ExcelRoster(RequestCycle cycle) {
        this.cycle = cycle;
    }

    RequestTarget generate(OrgProject prj,
            List<ProjectParticipation> participations,
            List<UserAccount> users) throws IOException {

        final Map<Long, UserAccount> userMap =
                CollectionsUtil.createMap(users, UserAccountTransformers.getUserIdTransformer());

        List<ParticipationUser> puserList = CollectionsUtil.transformList(participations,
                new Transformer<ProjectParticipation, ParticipationUser>() {
                    @Override
                    public ParticipationUser transform(ProjectParticipation item) {
                        return new ParticipationUser(userMap.get(item.getUserId()), item);
                    }
                });

        final Collator collator = Collator.getInstance(CocositeUserHelper.getUserLocale(cycle));
        collator.setStrength(Collator.SECONDARY);

        Collections.sort(puserList, new Comparator<ParticipationUser>() {
            @Override
            public int compare(ParticipationUser o1, ParticipationUser o2) {
                String u1 = o1.getName();
                String u2 = o2.getName();

                int diff = collator.compare(u1, u2);

                if (diff != 0) {
                    return diff;
                }

                return o1.getPart().compareTo(o2.getPart());
            }
        });

        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet("roster");

        final int startRow = 2;
        int rowNum = 0;

        addHeader(sheet);

        for (ParticipationUser participationUser : puserList) {
            Row row = sheet.createRow(startRow+rowNum);

            row.createCell(0).setCellValue(StringUtils.trimToEmpty(getFirstName(participationUser)));
            row.createCell(1).setCellValue(StringUtils.trimToEmpty(getLastName(participationUser)));
            row.createCell(2).setCellValue(participationUser.getPrimaryEmail());

            rowNum++;
        }

        sheet.setColumnWidth(0, 40*256);
        sheet.setColumnWidth(1, 40*256);
        sheet.setColumnWidth(2, 40*256);

        wb.write(baos);

        return new BytesRequestTarget(baos.toByteArray(), "application/vnd.ms-excel");
    }

    private void addHeader(Sheet sheet) {
        addTitleRow(sheet);
        addHeaderRow(sheet);
    }

    private void addTitleRow(Sheet sheet) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints((short)24);

        Font font = sheet.getWorkbook().createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short)16);
        font.setColor(HSSFColor.WHITE.index);

        CellStyle style = sheet.getWorkbook().createCellStyle();
        style.setLocked(true);
        style.setFont(font);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFillForegroundColor(HSSFColor.GREEN.index);
        style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        //style.setFillBackgroundColor(s);

        Cell cell = row.createCell(0);
        cell.setCellStyle(style);
        cell.setCellValue("Participant roster");

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
    }

    private void addHeaderRow(Sheet sheet) {
        Row row = sheet.createRow(1);
        row.setHeightInPoints((short)18);

        Font font = sheet.getWorkbook().createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short)12);
        font.setColor(HSSFColor.WHITE.index);

        CellStyle style = sheet.getWorkbook().createCellStyle();
        style.setLocked(true);
        style.setFont(font);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
        style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        //style.setFillBackgroundColor(s);

        addCell(row, 0, style, "First Name");
        addCell(row, 1, style, "Last Name");
        addCell(row, 2, style, "Email");
    }

    private void addCell(Row row, int col, CellStyle style, String cellValue) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        cell.setCellValue(cellValue);
    }

    private String getFirstName(ParticipationUser participationUser) {
        String fName = participationUser.getUser().getGivenName();

        if (StringUtils.isEmpty(fName)) {
            String displayName = participationUser.getUser().getDisplayName();
            int pos = displayName.lastIndexOf(' ');
            if (pos == -1) {
                return displayName;
            }

            return StringUtils.trim(displayName.substring(0, pos));
        }

        return fName;
    }

    private String getLastName(ParticipationUser participationUser) {
        String lName = participationUser.getUser().getSurname();

        if (StringUtils.isEmpty(lName)) {
            String displayName = participationUser.getUser().getDisplayName();
            int pos = displayName.lastIndexOf(' ');
            if (pos == -1) {
                return displayName;
            }

            return StringUtils.trim(displayName.substring(pos+1));
        }

        return lName;
    }

    private static class ParticipationUser {

        private final UserAccount user;
        private final ProjectParticipation part;

        public ParticipationUser(UserAccount user, ProjectParticipation part) {
            this.user = user;
            this.part = part;
        }

        public UserAccount getUser() {
            return user;
        }

        public ProjectParticipation getPart() {
            return part;
        }

        public String getName() {
            return user == null ? "Unknown" : user.getDisplayName();
        }

        public String getPrimaryEmail() {
            return user == null ? "Unknown" : user.getPrimaryEmail();
        }
    }
}
