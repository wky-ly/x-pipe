package com.ctrip.xpipe.redis.console.service.impl;

import com.ctrip.xpipe.api.monitor.Task;
import com.ctrip.xpipe.api.monitor.TransactionMonitor;
import com.ctrip.xpipe.api.organization.Organization;
import com.ctrip.xpipe.api.organization.OrganizationModel;
import com.ctrip.xpipe.redis.console.annotation.DalTransaction;
import com.ctrip.xpipe.redis.console.dao.OrganizationDao;
import com.ctrip.xpipe.redis.console.model.OrganizationTbl;
import com.ctrip.xpipe.redis.console.model.OrganizationTblDao;
import com.ctrip.xpipe.redis.console.service.AbstractConsoleService;
import com.ctrip.xpipe.redis.console.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * @author chen.zhu
 *
 * Sep 04, 2017
 */
@Service
public class OrganizationServiceImpl extends AbstractConsoleService<OrganizationTblDao>
    implements OrganizationService {

    @Autowired
    private OrganizationDao organizationDao;

    @Override
    public void updateOrganizations() {
        List<OrganizationTbl> remoteDBOrgs = retrieveOrgInfoFromRemote();
        List<OrganizationTbl> localDBOrgs = getAllOrganizations();
        List<OrganizationTbl> orgsToCreate = getOrgTblCreateList(remoteDBOrgs, localDBOrgs);
        List<OrganizationTbl> orgsToUpdate = getOrgTblUpdateList(remoteDBOrgs, localDBOrgs);
        updateDB(orgsToCreate, orgsToUpdate);
    }

    @DalTransaction
    private void updateDB(List<OrganizationTbl> orgsToCreate, List<OrganizationTbl> orgsToUpdate) {
        if (null != orgsToCreate && orgsToCreate.size() > 0) {
            logger.info("[handleUpdate][orgsToCreate]{}, {}", orgsToUpdate.size(), orgsToUpdate);
            organizationDao.createBatchOrganizations(orgsToCreate);
        }

        if (null != orgsToUpdate && orgsToUpdate.size() > 0) {
            logger.info("[handleUpdate][orgsToUpdate]{}, {}", orgsToUpdate.size(), orgsToUpdate);
            organizationDao.updateBatchOrganizations(orgsToUpdate);
        }
    }

    @Override
    public List<OrganizationTbl> getAllOrganizations() {
        return organizationDao.findAllOrgs()
            .stream().filter(org->org.getOrgId() != 0).collect(Collectors.toList());
    }

    @Override
    public OrganizationTbl getOrganizationTblByCMSOrganiztionId(long organizationId) {
        return organizationDao.findByOrgId(organizationId);
    }

    // Try to retrieve organization info from some source
    List<OrganizationTbl> retrieveOrgInfoFromRemote() {
        return TransactionMonitor.DEFAULT.logTransactionSwallowException("OrganizationService",
            "retrieveOrgInfoFromRemote", new Callable<List<OrganizationTbl>>() {

            @Override
            public List<OrganizationTbl> call() throws Exception {
                List<OrganizationModel> organizationModels = Organization.DEFAULT.retrieveOrganizationInfo();
                return organizationModels
                    .stream()
                    .map(org->
                         new OrganizationTbl().setOrgId(org.getId()).setOrgName(org.getName())
                    )
                    .collect(Collectors.toList());
            }
        });

    }

    List<OrganizationTbl> getOrgTblCreateList(List<OrganizationTbl> remoteDBOrgs,
        List<OrganizationTbl> localDBOrgs) {

        Set<Long> storedOrgId = new HashSet<>();
        localDBOrgs.forEach(org->storedOrgId.add(org.getOrgId()));
        return remoteDBOrgs.stream().filter(org->!storedOrgId.contains(org.getOrgId())).collect(Collectors.toList());
    }

    List<OrganizationTbl> getOrgTblUpdateList(List<OrganizationTbl> remoteDBOrgs,
        List<OrganizationTbl> localDBOrgs) {

        Map<Long, OrganizationTbl> storedOrgTbl = new HashMap<>();
        localDBOrgs.forEach(org->storedOrgTbl.put(org.getOrgId(), org));
        return remoteDBOrgs.stream().filter(org->storedOrgTbl.containsKey(org.getOrgId())
            && org.getOrgName() != storedOrgTbl.get(org.getOrgId()).getOrgName()).collect(Collectors.toList());
    }
}
