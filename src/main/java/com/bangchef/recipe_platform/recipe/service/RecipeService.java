package com.bangchef.recipe_platform.recipe.service;

import com.bangchef.recipe_platform.common.enums.RecipeCategory;
import com.bangchef.recipe_platform.common.enums.RecipeSortType;
import com.bangchef.recipe_platform.common.exception.CustomException;
import com.bangchef.recipe_platform.common.exception.ErrorCode;
import com.bangchef.recipe_platform.recipe.dto.RequestRecipeDto;
import com.bangchef.recipe_platform.recipe.dto.ResponseRecipeDto;
import com.bangchef.recipe_platform.recipe.entity.CookingStep;
import com.bangchef.recipe_platform.recipe.entity.Recipe;
import com.bangchef.recipe_platform.recipe.repository.CookingStepRepository;
import com.bangchef.recipe_platform.recipe.repository.RecipeRepository;
import com.bangchef.recipe_platform.user.dto.UserResponseDto;
import com.bangchef.recipe_platform.user.entity.User;
import com.bangchef.recipe_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final CookingStepRepository cookingStepRepository;
    private final UserRepository userRepository;

    @Transactional
    public Recipe createRecipe(RequestRecipeDto.Create requestDto, Long userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        Recipe recipe = Recipe.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .ingredients(requestDto.getIngredients())
                .category(requestDto.getCategory())
                .difficulty(requestDto.getDifficulty())
                .cookTime(requestDto.getCookTime())
                .imageUrl(requestDto.getImageUrl())
                .user(user)
                .build();

        Recipe savedRecipe = recipeRepository.save(recipe);

        List<CookingStep> cookingStepList = requestDto.getCookingStepDtoList().stream()
                .map(cookingStepDto -> CookingStep.builder()
                        .recipe(savedRecipe)
                        .stepNumber(cookingStepDto.getStepNumber())
                        .description(cookingStepDto.getDescription())
                        .imageUrl(cookingStepDto.getImageUrl())
                        .build())
                .toList();

        savedRecipe.setCookingStepList(cookingStepList);

        cookingStepRepository.saveAll(cookingStepList);

        return savedRecipe;
    }

    private ResponseRecipeDto.List convertToRecipeResponseDTO(Recipe recipe) {
        ResponseRecipeDto.List detail = new ResponseRecipeDto.List();

        detail.setId(recipe.getId());
        detail.setTitle(recipe.getTitle());
        detail.setCategory(recipe.getCategory());
        detail.setDifficulty(recipe.getDifficulty());
        detail.setCookTime(recipe.getCookTime());
        detail.setViews(recipe.getViews());
        detail.setFavoritesCount(recipe.getFavoritesCount());
        detail.setAvgRating(recipe.getAvgRating());
        detail.setImageUrl(recipe.getImageUrl());

        return detail;
    }

    public List<ResponseRecipeDto.List> findRecipeByTitle(String title, int page, RecipeSortType sortType){
        List<Recipe> recipeList = recipeRepository.findByTitle(title);

        if (recipeList.isEmpty()){
            throw new CustomException(ErrorCode.RECIPE_NOT_FOUND);
        }

        List<ResponseRecipeDto.List> recipeDtoList = new ArrayList<>();

        for (Recipe recipe : recipeList) {
            ResponseRecipeDto.List recipeDto = convertToRecipeResponseDTO(recipe);
            recipeDtoList.add(recipeDto);
        }

        sortBySortType(recipeDtoList, sortType);

        return getPagedRecipe(recipeDtoList, page);
    }

    public List<ResponseRecipeDto.List> findRecipeByCategory(RecipeCategory[] categories, int page, RecipeSortType sortType){
        HashMap<String, Integer> countingMap = new HashMap<>();
        Set<Recipe> recipeSet = new HashSet<>();

        if (categories.length == 0){
            throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        for (RecipeCategory category : categories){
            List<Recipe> recipeList = recipeRepository.findByCategory(category);

            if (recipeList.isEmpty()){
                throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
            }

            recipeSet.addAll(recipeList);

            for (Recipe recipe : recipeList){
                String title = recipe.getTitle();
                countingMap.put(title, countingMap.getOrDefault(title, 0) + 1);
            }
        }

        List<ResponseRecipeDto.List> recipeDtoList = new ArrayList<>();

        for (Recipe recipe : recipeSet){
            String title = recipe.getTitle();

            if (countingMap.containsKey(title) && countingMap.get(title) > 0){
                ResponseRecipeDto.List recipeDto = convertToRecipeResponseDTO(recipe);
                recipeDtoList.add(recipeDto);
            }
        }

        sortBySortType(recipeDtoList, sortType);

        return getPagedRecipe(recipeDtoList, page);
    }

    private static void sortBySortType(List<ResponseRecipeDto.List> recipeDtoList, RecipeSortType sortType){
        if (sortType == RecipeSortType.VIEWS_ASC){
            recipeDtoList.sort(new Comparator<ResponseRecipeDto.List>() {
                @Override
                public int compare(ResponseRecipeDto.List o1, ResponseRecipeDto.List o2) {
                    return o1.getViews() - o2.getViews();
                }
            });
        } else if (sortType == RecipeSortType.VIEWS_DES){
            recipeDtoList.sort(new Comparator<ResponseRecipeDto.List>() {
                @Override
                public int compare(ResponseRecipeDto.List o1, ResponseRecipeDto.List o2) {
                    return o2.getViews() - o1.getViews();
                }
            });
        } else if (sortType == RecipeSortType.RATING_ASC){
            recipeDtoList.sort(new Comparator<ResponseRecipeDto.List>() {
                @Override
                public int compare(ResponseRecipeDto.List o1, ResponseRecipeDto.List o2) {
                    return (int)(o1.getAvgRating() - o2.getAvgRating());
                }
            });
        } else if (sortType == RecipeSortType.RATING_DES){
            recipeDtoList.sort(new Comparator<ResponseRecipeDto.List>() {
                @Override
                public int compare(ResponseRecipeDto.List o1, ResponseRecipeDto.List o2) {
                    return (int)(o2.getViews() - o1.getViews());
                }
            });
        } else if (sortType == RecipeSortType.FAVORITES_ASC){
            recipeDtoList.sort(new Comparator<ResponseRecipeDto.List>() {
                @Override
                public int compare(ResponseRecipeDto.List o1, ResponseRecipeDto.List o2) {
                    return o1.getFavoritesCount() - o2.getFavoritesCount();
                }
            });
        } else if (sortType == RecipeSortType.FAVORITES_DES){
            recipeDtoList.sort(new Comparator<ResponseRecipeDto.List>() {
                @Override
                public int compare(ResponseRecipeDto.List o1, ResponseRecipeDto.List o2) {
                    return o2.getFavoritesCount() - o1.getFavoritesCount();
                }
            });
        }
    }

    private static List<ResponseRecipeDto.List> getPagedRecipe(List<ResponseRecipeDto.List> recipeDtoList, int page){
        List<ResponseRecipeDto.List> pagedRecipe = new ArrayList<>();
        final int PAGE_SIZE = 15;

        for (int i = PAGE_SIZE * page; i < PAGE_SIZE * (page + 1); i++){
            if (recipeDtoList.size() <= i){
                break;
            }

            pagedRecipe.add(recipeDtoList.get(i));
        }

        return pagedRecipe;
    }
}