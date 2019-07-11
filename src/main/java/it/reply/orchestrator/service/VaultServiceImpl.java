/*
 * Copyright © 2019 I.N.F.N.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

import it.reply.orchestrator.config.properties.VaultProperties;


@Service
@EnableConfigurationProperties(VaultProperties.class)
public class VaultServiceImpl implements VaultService {

    @Autowired
    private VaultProperties vaultProperties;
    
    /**
     * 
     * @param vaultProperties
     */
    public VaultServiceImpl(VaultProperties vaultProperties) {
        this.vaultProperties = vaultProperties;
    }
    
    private VaultTemplate getTemplate() {
        return new VaultTemplate(VaultEndpoint.create(
                vaultProperties.getUrl(),
                vaultProperties.getPort()),
            new TokenAuthentication(vaultProperties.getToken()));       
    }

    /**
     * 
     */
    public void writeSecret(String path, Object secret) {
        getTemplate().write(path, secret);
    }

    /**
     * 
     */
    public <T> T readSecret(String path, Class<T> type) {        
        return getTemplate().read(path, type).getData();
    }

    /**
     * 
     */
    public Map<String,Object> readSecret(String path) {
        return getTemplate().read(path).getData();
    }

    /**
     * 
     */
    public void deleteSecret(String path) {
        getTemplate().delete(path);
    }

    /**
     * 
     */
    public List<String> listSecrets(String path) {
        return getTemplate().list(path);
    }

}
