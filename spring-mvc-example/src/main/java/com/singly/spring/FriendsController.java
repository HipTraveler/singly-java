package com.singly.spring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.singly.client.SinglyAccountStorage;
import com.singly.client.SinglyService;
import com.singly.util.JSON;

@Controller
@RequestMapping("/friends.html")
public class FriendsController {

  @Autowired
  private SinglyService singlyService;

  @Autowired
  private SinglyAccountStorage accountStorage;

  public class Friend {

    public String name;
    public List<String> serviceIds;
    public String imageUrl;
    public String profileUrl;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getServiceIds() {
      return serviceIds;
    }

    public void setServiceIds(List<String> serviceIds) {
      this.serviceIds = serviceIds;
    }

    public String getImageUrl() {
      return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
    }

    public String getProfileUrl() {
      return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
      this.profileUrl = profileUrl;
    }

  }

  private int getFriendCount(String account) {

    // query parameters for the api call, add in access token
    Map<String, String> qparams = new LinkedHashMap<String, String>();
    qparams.put("access_token", accountStorage.getAccessToken(account));

    // make an API call to get profiles data and add the JSON to the model
    String friendCountJson = singlyService.doGetApiRequest("/friends", qparams);
    JsonNode root = JSON.parse(friendCountJson);
    return JSON.getInt(root, "all");
  }

  private List<Friend> getFriends(String account) {

    List<Friend> friends = new ArrayList<Friend>();
    int numFriends = getFriendCount(account);
    int blockSize = 20;

    int blocks = numFriends / blockSize;
    if (blocks % blockSize > 0) {
      blocks += 1;
    }

    for (int i = 0; i < blocks; i++) {

      int offset = (i * blockSize);
      int limit = blockSize;

      Map<String, String> qparams = new LinkedHashMap<String, String>();
      qparams.put("access_token", accountStorage.getAccessToken(account));
      qparams.put("offset", String.valueOf(offset));
      qparams.put("limit", String.valueOf(limit));
      qparams.put("toc", "false");

      // make an API call to get profiles data and add the JSON to the model
      String friendsJson = singlyService.doGetApiRequest("/friends/all",
        qparams);

      JsonNode root = JSON.parse(friendsJson);

      for (JsonNode node : root) {

        // parse the friend and add it to the block
        Friend friend = new Friend();
        friend.name = JSON.getString(node, "name");
        friend.profileUrl = JSON.getString(node, "url");
        friend.imageUrl = JSON.getString(node, "thumbnail_url");

        JsonNode services = JSON.getJsonNode(node, "services");
        List<String> serviceIds = JSON.getFieldnames(services);
        friend.setServiceIds(serviceIds);

        friends.add(friend);
      }
    }

    return friends;
  }

  @RequestMapping(method = RequestMethod.GET)
  public String getView(Model model, HttpServletRequest request,
    HttpServletResponse response) {

    // store the account in the session
    HttpSession session = request.getSession();
    String account = (String)session.getAttribute("account");

    // redirect is not authenticated
    if (StringUtils.isBlank(account) || !singlyService.isAuthenticated(account)) {
      return "redirect:/authentication.html";
    }

    List<Friend> friends = getFriends(account);
    model.addAttribute("friends", friends);

    return "/friends";
  }
}
