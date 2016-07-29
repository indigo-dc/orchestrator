package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.ProviderDetails;
import it.reply.orchestrator.dto.onedata.SpaceDetails;
import it.reply.orchestrator.dto.onedata.UserSpaces;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OneDataService {

  public String getServiceSpaceToken();

  public String getServiceSpaceName();

  public String getServiceSpaceProvider();

  public String getServiceSpacePath();

  public UserSpaces getUserSpacesId(String oneZoneEndpoint, String onedataToken);

  public UserSpaces getUserSpacesId(String onedataToken);

  public String getUserSpaceNameById(String oneZoneEndpoint, String onedataToken,
      String oneSpaceId);

  public String getUserSpaceNameById(String onedataToken, String oneSpaceId);

  public List<String> getProvidersIdBySpaceId(String oneZoneEndpoint, String onedataToken,
      String oneSpaceId);

  public List<String> getProvidersIdBySpaceId(String onedataToken, String oneSpaceId);

  public SpaceDetails getSpaceDetailsFromId(String oneZoneEndpoint, String oneDataToken,
      String oneSpaceId);

  public SpaceDetails getSpaceDetailsFromId(String oneDataToken, String oneSpaceId);

  public ProviderDetails getProviderDetailsFromId(String oneZoneEndpoint, String oneDataToken,
      String oneSpaceId, String oneProviderId);

  public ProviderDetails getProviderDetailsFromId(String oneDataToken, String oneSpaceId,
      String oneProviderId);

  public Map<OneData, Set<String>> getProvidersId(Collection<OneData> oneDataParams);

}
