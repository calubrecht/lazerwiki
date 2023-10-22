package us.calubrecht.lazerwiki.service;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(rollbackFor = PageWriteException.class )
public class PageService {

    @Autowired
    PageRepository pageRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;



    public boolean exists(String pageName) {
        return false;
    }

    public String getTitle(String pageName) {
        if (!exists(pageName)) {
            return Arrays.stream(pageName.split(":")).reduce((first, second) -> second)
                    .orElse(null);
        }
        return "";
    }

    public String getSource(String host, String sPageDescriptor, String userName) {
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("default", pageDescriptor.namespace(), pageDescriptor.pageName(), false);
        if (p == null ) {
            return "This page doesn't exist";
        }
        return p.getText().formatted(userName, pageDescriptor);
    }

    public PageDescriptor decodeDescriptor(String pageDescriptor) {
        List<String> tokens = new ArrayList<>(Arrays.asList(pageDescriptor.split(":")));
        if (tokens.size() == 0) {
            return new PageDescriptor("","");
        }
        String pageName = tokens.remove(tokens.size() -1);
        return new PageDescriptor(String.join(":", tokens), pageName);
    }

    public void savePage(String host, String sPageDescriptor, String text) throws PageWriteException{
        // get Existing
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("default", pageDescriptor.namespace(), pageDescriptor.pageName(), false);
        long id = p == null ? getNewId() : p.getId();
        long revision = p == null ? 1 : p.getRevision() + 1;
        if (p != null ) {
            // invalidate old page.
            pageRepository.save(p);
        }
        Page newP = new Page();
        newP.setSite("default");
        newP.setNamespace(pageDescriptor.namespace());
        newP.setPagename(pageDescriptor.pageName());
        newP.setText(text);
        newP.setId(id);
        newP.setRevision(revision);
        pageRepository.save(newP);
    }

    protected long getNewId() {

        final String generateIdSQL = "INSERT INTO lazerwiki.page_ids () VALUES ()";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                new PreparedStatementCreator() {
                    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                        return connection.prepareStatement(generateIdSQL, new String[] {"id"});
                    }
                }, keyHolder);
        return (long)keyHolder.getKey();
    }
}
